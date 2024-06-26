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

import config.annotations.NonEeaBusiness
import connectors.SubmissionDraftConnector
import controllers.actions._
import controllers.filters.IndexActionFilterProvider
import forms.StartDateFormProvider
import models.requests.RegistrationDataRequest
import navigation.Navigator
import pages.asset.noneeabusiness.{NamePage, StartDatePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.asset.noneeabusiness.StartDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartDateController @Inject() (
  override val messagesApi: MessagesApi,
  repository: RegistrationsRepository,
  @NonEeaBusiness navigator: Navigator,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  validateIndex: IndexActionFilterProvider,
  requireData: RegistrationDataRequiredAction,
  requiredAnswer: RequiredAnswerActionProvider,
  formProvider: StartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: StartDateView,
  submissionDraftConnector: SubmissionDraftConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def messageKeyPrefix: String = "nonEeaBusiness.startDate"

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen
      getData(draftId) andThen
      requireData andThen
      validateIndex(index, sections.Assets) andThen
      requiredAnswer(RequiredAnswer(NamePage(index), routes.NameController.onPageLoad(index, draftId)))

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val name = request.userAnswers.get(NamePage(index)).get

    submissionDraftConnector.getTrustSetupDate(draftId) map { trustSetupDate =>
      val form = formProvider.withConfig(messageKeyPrefix, trustSetupDate)

      val preparedForm = request.userAnswers.get(StartDatePage(index)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, index, draftId, name))
    }
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    val name = request.userAnswers.get(NamePage(index)).get

    submissionDraftConnector.getTrustSetupDate(draftId) flatMap { trustSetupDate =>
      val form = formProvider.withConfig(messageKeyPrefix, trustSetupDate)

      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[_]) => Future.successful(BadRequest(view(formWithErrors, index, draftId, name))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(StartDatePage(index), value))
              _              <- repository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(StartDatePage(index), draftId)(updatedAnswers))
        )
    }
  }
}
