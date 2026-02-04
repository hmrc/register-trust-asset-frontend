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

package controllers.asset.other

import controllers.actions._
import mapping.reads.OtherAsset
import models.Status.Completed
import models.UserAnswers
import models.requests.RegistrationDataRequest
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.other.{OtherAssetDescriptionPage, OtherAssetPage, OtherAssetValuePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsPath
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.print.OtherPrintHelper
import viewmodels.{AssetViewModel, OtherAssetViewModel}
import views.html.asset.other.OtherAssetAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class OtherAssetAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  requiredAnswer: RequiredAnswerActionProvider,
  view: OtherAssetAnswersView,
  val controllerComponents: MessagesControllerComponents,
  printHelper: OtherPrintHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen getData(draftId) andThen requireData andThen
      requiredAnswer(
        RequiredAnswer(
          WhatKindOfAssetPage(index),
          controllers.asset.routes.WhatKindOfAssetController.onPageLoad(index, draftId)
        )
      )      andThen
      requiredAnswer(
        RequiredAnswer(
          OtherAssetDescriptionPage(index),
          routes.OtherAssetDescriptionController.onPageLoad(index, draftId)
        )
      )      andThen
      requiredAnswer(
        RequiredAnswer(OtherAssetValuePage(index), routes.OtherAssetValueController.onPageLoad(index, draftId))
      )

  private def getExistingOtherAssets(excludeIndex: Int, userAnswers: UserAnswers): Seq[OtherAsset] = {
    val allAssets: List[AssetViewModel] = userAnswers.get(sections.Assets).getOrElse(Nil)
    allAssets.zipWithIndex.collect {
      case (_: OtherAssetViewModel, i) if i != excludeIndex =>
        userAnswers.get(OtherAssetPage(i))
    }.flatten
  }

  private def isDuplicate(current: OtherAsset, existingAssets: Seq[OtherAsset]): Boolean =
    existingAssets.exists { existing =>
      existing.description.equalsIgnoreCase(current.description) &&
      existing.value == current.value
    }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) { implicit request =>
    val description = request.userAnswers.get(OtherAssetDescriptionPage(index)).get

    val section = printHelper.checkDetailsSection(
      userAnswers = request.userAnswers,
      arg = description,
      index = index,
      draftId = draftId
    )

    Ok(view(index, draftId, section))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val maybeCurrentAsset: Option[OtherAsset] = request.userAnswers.get(OtherAssetPage(index))
    val existingAssets: Seq[OtherAsset]       = getExistingOtherAssets(excludeIndex = index, request.userAnswers)

    maybeCurrentAsset match {
      case Some(current) if isDuplicate(current, existingAssets) =>
        logger.info("duplicate other asset not added")
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
        } yield Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId))
      case None                                                  =>
        Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
    }
  }

}
