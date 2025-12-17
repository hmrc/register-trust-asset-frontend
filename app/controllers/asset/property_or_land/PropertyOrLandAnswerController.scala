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

package controllers.asset.property_or_land

import config.annotations.PropertyOrLand
import controllers.actions._
import mapping.reads.PropertyOrLandAsset
import models.Status.Completed
import models.UserAnswers
import navigation.Navigator
import pages.AssetStatus
import pages.asset.property_or_land.{PropertyOrLandAnswerPage, PropertyOrLandAssetPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsPath
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.print.PropertyOrLandPrintHelper
import viewmodels.{AssetViewModel, PropertyOrLandAssetViewModel}
import views.html.asset.property_or_land.PropertyOrLandAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PropertyOrLandAnswerController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  @PropertyOrLand navigator: Navigator,
  actions: Actions,
  view: PropertyOrLandAnswersView,
  val controllerComponents: MessagesControllerComponents,
  printHelper: PropertyOrLandPrintHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def getExistingPropertyOrLandAssets(excludeIndex: Int, userAnswers: UserAnswers): Seq[PropertyOrLandAsset] = {
    val allAssets: List[AssetViewModel] = userAnswers.get(sections.Assets).getOrElse(Nil)
    allAssets.zipWithIndex.collect {
      case (_: PropertyOrLandAssetViewModel, i) if i != excludeIndex =>
        userAnswers.get(PropertyOrLandAssetPage(i))
    }.flatten
  }

  private def isDuplicate(current: PropertyOrLandAsset, existingAssets: Seq[PropertyOrLandAsset]): Boolean =
    existingAssets.exists { existing =>
      existing.propertyOrLandDescription == current.propertyOrLandDescription &&
      existing.address == current.address &&
      existing.propertyLandValueTrust == current.propertyLandValueTrust &&
      existing.propertyOrLandTotalValue == current.propertyOrLandTotalValue
    }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions.authWithData(draftId) { implicit request =>
    val sections = printHelper.checkDetailsSection(
      userAnswers = request.userAnswers,
      index = index,
      draftId = draftId
    )

    Ok(view(index, draftId, sections))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] =
    actions.authWithData(draftId).async { implicit request =>
      val maybeCurrentAsset: Option[PropertyOrLandAsset] = request.userAnswers.get(PropertyOrLandAssetPage(index))
      val existingAssets: Seq[PropertyOrLandAsset]       =
        getExistingPropertyOrLandAssets(excludeIndex = index, request.userAnswers)

      maybeCurrentAsset match {
        case Some(current) if isDuplicate(current, existingAssets) =>
          logger.info("duplicate property or land asset not added")
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
          } yield Redirect(navigator.nextPage(PropertyOrLandAnswerPage, draftId)(request.userAnswers))
        case None                                                  =>
          Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
      }

    }
}
