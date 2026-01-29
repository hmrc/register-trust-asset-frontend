/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.asset

import config.annotations.Asset
import controllers.actions.{
  DraftIdRetrievalActionProvider, RegistrationDataRequiredAction, RegistrationIdentifierAction
}
import forms.{AddAssetsFormProvider, YesNoFormProvider}
import models.AddAssets.NoComplete
import models.Constants._
import models.Status.Completed
import models.TaskStatus.TaskStatus
import models.requests.RegistrationDataRequest
import models.{AddAssets, TaskStatus, UserAnswers}
import navigation.Navigator
import pages.RegistrationProgress
import pages.asset.{AddAnAssetYesNoPage, AddAssetsPage}
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi, MessagesProvider}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import services.TrustsStoreService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AddAssetViewHelper
import views.html.asset.{AddAnAssetYesNoView, AddAssetsView, MaxedOutView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddAssetsController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  @Asset navigator: Navigator,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  addAnotherFormProvider: AddAssetsFormProvider,
  yesNoFormProvider: YesNoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  addAssetsView: AddAssetsView,
  yesNoView: AddAnAssetYesNoView,
  maxedOutView: MaxedOutView,
  trustsStoreService: TrustsStoreService,
  registrationProgress: RegistrationProgress
)(implicit ec: ExecutionContext)
    extends AddAssetController {

  private def addAnotherForm(isTaxable: Boolean): Form[AddAssets] =
    addAnotherFormProvider.withPrefix(determinePrefix(isTaxable))

  private val yesNoForm: Form[Boolean]                            = yesNoFormProvider.withPrefix("addAnAssetYesNo")

  private def actions(draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen getData(draftId) andThen requireData

  private def determinePrefix(isTaxable: Boolean): String = "addAssets" + (if (!isTaxable) ".nonTaxable" else "")

  private def heading(count: Int, prefix: String)(implicit mp: MessagesProvider): String =
    if (count > 1 && prefix != "addAssets.nonTaxable") {
      Messages(s"$prefix.count.heading", count)
    } else {
      Messages(s"$prefix.heading")
    }

  private def setTaskStatus(draftId: String, userAnswers: UserAnswers, action: AddAssets)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val status = (action, registrationProgress.assetsStatus(userAnswers)) match {
      case (NoComplete, Some(Completed)) => TaskStatus.Completed
      case _                             => TaskStatus.InProgress
    }
    setTaskStatus(draftId, status)
  }

  private def setTaskStatus(draftId: String, taskStatus: TaskStatus)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    trustsStoreService.updateTaskStatus(draftId, taskStatus)

  def onPageLoad(draftId: String): Action[AnyContent] = actions(draftId) { implicit request =>
    val userAnswers: UserAnswers = request.userAnswers
    val isTaxable                = userAnswers.isTaxable

    val rows = new AddAssetViewHelper(userAnswers, draftId).rows

    val prefix = determinePrefix(isTaxable)

    if (userAnswers.assets.nonMaxedOutOptions.isEmpty) {
      val maxLimit: Int = if (isTaxable) {
        MAX_TAXABLE_ASSETS
      } else {
        MAX_NON_TAXABLE_ASSETS
      }

      Ok(maxedOutView(draftId, rows.inProgress, rows.complete, heading(rows.count, prefix), maxLimit, prefix))
    } else {
      if (rows.nonEmpty) {
        Ok(
          addAssetsView(
            addAnotherForm(isTaxable),
            draftId,
            rows.inProgress,
            rows.complete,
            heading(rows.count, prefix),
            prefix,
            userAnswers.assets.maxedOutOptions
          )
        )
      } else {
        if (isTaxable) {
          Ok(yesNoView(yesNoForm, draftId))
        } else {
          Redirect(routes.TrustOwnsNonEeaBusinessYesNoController.onPageLoad(draftId))
        }
      }
    }
  }

  def submitOne(draftId: String): Action[AnyContent] = actions(draftId).async { implicit request =>
    yesNoForm
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[_]) => Future.successful(BadRequest(yesNoView(formWithErrors, draftId))),
        value =>
          for {
            answersWithAssetTypeIfNonTaxable <- Future.fromTry(setAssetType(request.userAnswers, 0))
            updatedAnswers                   <- Future.fromTry(answersWithAssetTypeIfNonTaxable.set(AddAnAssetYesNoPage, value))
            _                                <- repository.set(updatedAnswers)
            taskStatus                       <- Future.successful {
                                                  if (updatedAnswers.isTaxable) {
                                                    TaskStatus.InProgress
                                                  } else {
                                                    // Non taxable does not require an asset
                                                    if (value) TaskStatus.InProgress else TaskStatus.Completed
                                                  }
                                                }
            _                                <- setTaskStatus(draftId, taskStatus)
          } yield Redirect(navigator.nextPage(AddAnAssetYesNoPage, draftId)(updatedAnswers))
      )
  }

  def submitAnother(draftId: String): Action[AnyContent] = actions(draftId).async { implicit request =>
    val userAnswers = request.userAnswers
    val isTaxable   = userAnswers.isTaxable

    val rows = new AddAssetViewHelper(userAnswers, draftId).rows

    val prefix = determinePrefix(isTaxable)

    addAnotherForm(isTaxable)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[_]) =>
          Future.successful(
            BadRequest(
              addAssetsView(
                formWithErrors,
                draftId,
                rows.inProgress,
                rows.complete,
                heading(rows.count, prefix),
                prefix,
                userAnswers.assets.maxedOutOptions
              )
            )
          ),
        value =>
          for {
            answersWithAssetTypeIfNonTaxable <- Future.fromTry(setAssetType(userAnswers, rows.count, value))
            updatedAnswers                   <- Future.fromTry(answersWithAssetTypeIfNonTaxable.set(AddAssetsPage, value))
            _                                <- repository.set(updatedAnswers)
            _                                <- setTaskStatus(draftId, updatedAnswers, value)
          } yield Redirect(navigator.nextPage(AddAssetsPage, draftId)(updatedAnswers))
      )
  }

  def submitComplete(draftId: String): Action[AnyContent] = actions(draftId).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAssetsPage, NoComplete))
      _              <- repository.set(updatedAnswers)
      _              <- setTaskStatus(draftId, updatedAnswers, NoComplete)
    } yield Redirect(navigator.nextPage(AddAssetsPage, draftId)(updatedAnswers))
  }

}
