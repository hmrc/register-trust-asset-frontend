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

package controllers.asset.shares

import config.annotations.Shares
import controllers.actions._
import mapping.reads.{ShareAsset, ShareNonPortfolioAsset, SharePortfolioAsset}
import models.Status.Completed
import models.UserAnswers
import models.requests.RegistrationDataRequest
import navigation.Navigator
import pages.AssetStatus
import pages.asset.shares.{ShareAnswerPage, ShareAssetPage, ShareCompanyNamePage, SharePortfolioNamePage, SharesInAPortfolioPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsPath
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import queries.Gettable
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.print.SharesPrintHelper
import viewmodels.{AssetViewModel, ShareAssetViewModel}
import views.html.asset.shares.ShareAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ShareAnswerController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  @Shares navigator: Navigator,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  requiredAnswer: RequiredAnswerActionProvider,
  view: ShareAnswersView,
  val controllerComponents: MessagesControllerComponents,
  printHelper: SharesPrintHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen
      getData(draftId) andThen
      requireData andThen
      requiredAnswer(
        RequiredAnswer(SharesInAPortfolioPage(index), routes.SharesInAPortfolioController.onPageLoad(index, draftId))
      )

  private def getExistingShareAssets(excludeIndex: Int, userAnswers: UserAnswers): Seq[ShareAsset] = {
    val allAssets: List[AssetViewModel] = userAnswers.get(sections.Assets).getOrElse(Nil)
    allAssets.zipWithIndex.collect {
      case (_: ShareAssetViewModel, i) if i != excludeIndex =>
        userAnswers.get(ShareAssetPage(i))
    }.flatten
  }

  private def isDuplicate(current: ShareAsset, existingAssets: Seq[ShareAsset]): Boolean =
    existingAssets.exists { existing =>
      existing.name.equalsIgnoreCase(current.name) &&
      existing.listedOnTheStockExchange == current.listedOnTheStockExchange &&
      existing.quantityInTheTrust == current.quantityInTheTrust &&
      areSameShareType(existing, current)
    }

  private def areSameShareType(existing: ShareAsset, current: ShareAsset): Boolean =
    (existing, current) match {
      case (e: ShareNonPortfolioAsset, c: ShareNonPortfolioAsset) =>
        e.`class` == c.`class` && e.value == c.value
      case (e: SharePortfolioAsset, c: SharePortfolioAsset)       =>
        e.value == c.value
      case _                                                      =>
        false
    }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) { implicit request =>
    def getPage(page: Gettable[String]): Option[String] =
      request.userAnswers.get(page)

    val name: String = (getPage(ShareCompanyNamePage(index)), getPage(SharePortfolioNamePage(index))) match {
      case (Some(name), None) => name
      case (None, Some(name)) => name
      case _                  => request.messages(messagesApi)("assets.defaultText")
    }

    val sections = printHelper.checkDetailsSection(
      userAnswers = request.userAnswers,
      arg = name,
      index = index,
      draftId = draftId
    )

    Ok(view(index, draftId, sections))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val maybeCurrentAsset: Option[ShareAsset] = request.userAnswers.get(ShareAssetPage(index))
    val existingAssets: Seq[ShareAsset]       = getExistingShareAssets(excludeIndex = index, request.userAnswers)

    maybeCurrentAsset match {
      case Some(current) if isDuplicate(current, existingAssets) =>
        logger.info("duplicate share asset not added")
        val removePath = JsPath \ "assets" \ index
        request.userAnswers.deleteAtPath(removePath) match {
          case Success(cleanedUA) =>
            repository
              .set(cleanedUA)
              .map(_ => Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
          case Failure(_)         =>
            Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
        }
      case Some(_)                                               =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(AssetStatus(index), Completed))
          _              <- repository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ShareAnswerPage, draftId)(request.userAnswers))
      case None                                                  =>
        Future.successful(Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId)))
    }
  }
}
