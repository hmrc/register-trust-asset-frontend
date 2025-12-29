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

package controllers.asset.partnership

import config.annotations.Partnership
import controllers.actions._
import mapping.reads.PartnershipAsset
import models.Status.Completed
import models.UserAnswers
import models.requests.RegistrationDataRequest
import navigation.Navigator
import pages.AssetStatus
import pages.asset.partnership._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsPath
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.print.PartnershipPrintHelper
import viewmodels.{AssetViewModel, PartnershipAssetViewModel}
import views.html.asset.partnership.PartnershipAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PartnershipAnswerController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  @Partnership navigator: Navigator,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  requiredAnswer: RequiredAnswerActionProvider,
  view: PartnershipAnswersView,
  val controllerComponents: MessagesControllerComponents,
  printHelper: PartnershipPrintHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen
      getData(draftId) andThen
      requireData andThen
      requiredAnswer(
        RequiredAnswer(
          PartnershipDescriptionPage(index),
          routes.PartnershipDescriptionController.onPageLoad(index, draftId)
        )
      ) andThen
      requiredAnswer(
        RequiredAnswer(
          PartnershipStartDatePage(index),
          routes.PartnershipStartDateController.onPageLoad(index, draftId)
        )
      )

  private def getExistingPartnershipAssets(excludeIndex: Int, userAnswers: UserAnswers): Seq[PartnershipAsset] = {
    val allAssets: List[AssetViewModel] = userAnswers.get(sections.Assets).getOrElse(Nil)
    allAssets.zipWithIndex.collect {
      case (_: PartnershipAssetViewModel, i) if i != excludeIndex =>
        userAnswers.get(PartnershipAssetPage(i))
    }.flatten
  }

  private def isDuplicate(current: PartnershipAsset, existingAssets: Seq[PartnershipAsset]): Boolean =
    existingAssets.exists { existing =>
      existing.description.equalsIgnoreCase(current.description) &&
      existing.startDate == current.startDate
    }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) { implicit request =>
    val sections = printHelper.checkDetailsSection(
      userAnswers = request.userAnswers,
      index = index,
      draftId = draftId
    )

    Ok(view(index, draftId, sections))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val maybeCurrentAsset: Option[PartnershipAsset] = request.userAnswers.get(PartnershipAssetPage(index))
    val existingAssets: Seq[PartnershipAsset]       = getExistingPartnershipAssets(excludeIndex = index, request.userAnswers)

    maybeCurrentAsset match {
      case Some(current) if isDuplicate(current, existingAssets) =>
        logger.info("duplicate partnership asset not added")
        val removePath = JsPath \ "assets" \ index
        request.userAnswers.deleteAtPath(removePath) match {
          case Success(cleanedUA) =>
            repository
              .set(cleanedUA)
              .map(_ => Redirect(controllers.asset.routes.DuplicateAssetController.onPageLoad(draftId)))
          case Failure(_)         =>
            Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
        }
      case Some(_)                                               =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(AssetStatus(index), Completed))
          _              <- repository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(PartnershipAnswerPage, draftId)(request.userAnswers))
      case None                                                  =>
        Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
    }
  }
}
