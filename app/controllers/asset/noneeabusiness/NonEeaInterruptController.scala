/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.actions._
import controllers.filters.IndexActionFilterProvider
import models.requests.RegistrationDataRequest
import navigation.Navigator
import pages.asset.NonEeaInterruptPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import sections.Assets
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.countryOptions.CountryOptionsNonUK
import views.html.asset.noneeabusiness.NonEeaInterruptView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonEeaInterruptController @Inject() (
  override val messagesApi: MessagesApi,
  @NonEeaBusiness navigator: Navigator,
  validateIndex: IndexActionFilterProvider,
  identify: RegistrationIdentifierAction,
  getData: DraftIdRetrievalActionProvider,
  requireData: RegistrationDataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: NonEeaInterruptView,
  val countryOptions: CountryOptionsNonUK
) extends FrontendBaseController
    with I18nSupport {

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen
      getData(draftId) andThen
      requireData andThen
      validateIndex(index, Assets)

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) { implicit request =>
    Ok(view(index, draftId))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async { implicit request =>
    Future.successful(
      Redirect(
        navigator.nextPage(NonEeaInterruptPage(index), draftId)(request.userAnswers)
      )
    )
  }
}
