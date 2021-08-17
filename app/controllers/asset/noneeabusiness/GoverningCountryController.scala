/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.CountryFormProvider
import models.requests.RegistrationDataRequest
import navigation.Navigator
import pages.asset.noneeabusiness.{GoverningCountryPage, NamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import sections.Assets
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.countryOptions.CountryOptions
import views.html.asset.noneeabusiness.GoverningCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoverningCountryController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            registrationsRepository: RegistrationsRepository,
                                            @NonEeaBusiness navigator: Navigator,
                                            validateIndex: IndexActionFilterProvider,
                                            identify: RegistrationIdentifierAction,
                                            getData: DraftIdRetrievalActionProvider,
                                            requireData: RegistrationDataRequiredAction,
                                            requiredAnswer: RequiredAnswerActionProvider,
                                            countryFormProvider: CountryFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: GoverningCountryView,
                                            countryOptions: CountryOptions
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form: Form[String] = countryFormProvider.withPrefix("nonEeaBusiness.governingCountry")

  private def actions(index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] =
    identify andThen
      getData(draftId) andThen
      requireData andThen
      validateIndex(index, Assets) andThen
      requiredAnswer(RequiredAnswer(NamePage(index), routes.NameController.onPageLoad(index, draftId)))

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId) {
    implicit request =>

      val name = request.userAnswers.get(NamePage(index)).get

      val preparedForm = request.userAnswers.get(GoverningCountryPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val isTaxable = request.userAnswers.isTaxable

      Ok(view(preparedForm, countryOptions.options(), draftId, index, name, isTaxable))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(index, draftId).async {
    implicit request =>

      val name = request.userAnswers.get(NamePage(index)).get
      val isTaxable = request.userAnswers.isTaxable

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, countryOptions.options(), draftId, index, name, isTaxable))),

        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(GoverningCountryPage(index), value))
            _ <- registrationsRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(GoverningCountryPage(index), draftId)(updatedAnswers))
        }
      )
  }
}
