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

package controllers.asset.noneeabusiness

import controllers.actions._
import mapping.reads.NonEeaBusinessAsset
import models.Status.Completed
import models.UserAnswers
import models.requests.RegistrationDataRequest
import pages.AssetStatus
import pages.asset.noneeabusiness.{NamePage, NonEeaBusinessAssetPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsPath
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.print.NonEeaBusinessPrintHelper
import viewmodels.{AssetViewModel, NonEeaBusinessAssetViewModel}
import views.html.asset.noneeabusiness.AnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AnswersController @Inject() (
  override val messagesApi: MessagesApi,
  registrationsRepository: RegistrationsRepository,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  requiredAnswer: RequiredAnswerActionProvider,
  view: AnswersView,
  val controllerComponents: MessagesControllerComponents,
  printHelper: NonEeaBusinessPrintHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify           andThen
      getData(draftId) andThen
      requireData      andThen
      requiredAnswer(RequiredAnswer(NamePage(index), routes.NameController.onPageLoad(index, draftId)))

  private def getExistingNonEeaBusinessAssets(excludeIndex: Int, userAnswers: UserAnswers): Seq[NonEeaBusinessAsset] = {
    val allAssets: List[AssetViewModel] = userAnswers.get(sections.Assets).getOrElse(Nil)
    allAssets.zipWithIndex.collect {
      case (_: NonEeaBusinessAssetViewModel, i) if i != excludeIndex =>
        userAnswers.get(NonEeaBusinessAssetPage(i))
    }.flatten
  }

  private def isDuplicate(current: NonEeaBusinessAsset, existingAssets: Seq[NonEeaBusinessAsset]): Boolean =
    existingAssets.exists { existing =>
      existing.name.equalsIgnoreCase(current.name) &&
      existing.address.line1.equalsIgnoreCase(current.address.line1) &&
      existing.governingCountry.equalsIgnoreCase(current.governingCountry) &&
      existing.startDate == current.startDate
    }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) { implicit request =>
    val name = request.userAnswers.get(NamePage(index)).get

    val section = printHelper.checkDetailsSection(
      userAnswers = request.userAnswers,
      arg = name,
      index = index,
      draftId = draftId
    )

    Ok(view(index, draftId, section))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val maybeCurrentAsset: Option[NonEeaBusinessAsset] = request.userAnswers.get(NonEeaBusinessAssetPage(index))
    val existingAssets: Seq[NonEeaBusinessAsset]       =
      getExistingNonEeaBusinessAssets(excludeIndex = index, request.userAnswers)

    maybeCurrentAsset match {
      case Some(current) if isDuplicate(current, existingAssets) =>
        logger.info("duplicate non-EEA business asset not added")
        val removePath = JsPath \ "assets" \ index
        request.userAnswers.deleteAtPath(removePath) match {
          case Success(cleanedUA) =>
            registrationsRepository
              .set(cleanedUA)
              .map(_ => Redirect(controllers.asset.routes.DuplicateAssetController.onPageLoad(draftId)))
          case Failure(_)         =>
            Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
        }
      case Some(_)                                               =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(AssetStatus(index), Completed))
          _              <- registrationsRepository.set(updatedAnswers)
        } yield Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId))
      case None                                                  =>
        Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
    }
  }

}
