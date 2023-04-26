/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import connectors.SubmissionDraftConnector
import controllers.actions.RegistrationIdentifierAction
import controllers.asset.routes._
import models.{TaskStatus, UserAnswers}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.RegistrationsRepository
import services.TrustsStoreService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.AssetViewModel

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  repository: RegistrationsRepository,
  identify: RegistrationIdentifierAction,
  submissionDraftConnector: SubmissionDraftConnector,
  trustsStoreService: TrustsStoreService
)(implicit appConfig: FrontendAppConfig, ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def updateTaskStatus(draftId: String, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Result] = for {
    _      <- repository.set(userAnswers)
    assets <- Future.successful(userAnswers.get(sections.Assets).toList.flatten)
    _      <- trustsStoreService.updateTaskStatus(draftId, TaskStatus.InProgress)
  } yield navigate(draftId, assets, userAnswers.isTaxable)

  private def navigate(draftId: String, assets: List[AssetViewModel], isTaxable: Boolean): Result =
    assets match {
      case Nil =>
        if (isTaxable) {
          Redirect(AssetInterruptPageController.onPageLoad(draftId))
        } else {
          Redirect(TrustOwnsNonEeaBusinessYesNoController.onPageLoad(draftId))
        }
      case _   =>
        Redirect(AddAssetsController.onPageLoad(draftId))
    }

  def onPageLoad(draftId: String): Action[AnyContent] = identify.async { implicit request =>
    for {
      isTaxable   <- submissionDraftConnector.getIsTrustTaxable(draftId)
      userAnswers <- repository.get(draftId)
      result      <- userAnswers match {
                       case Some(answers) =>
                         updateTaskStatus(draftId, answers.copy(isTaxable = isTaxable))
                       case None          =>
                         val userAnswers = UserAnswers(draftId, Json.obj(), request.identifier, isTaxable)
                         updateTaskStatus(draftId, userAnswers)
                     }
    } yield result
  }
}
