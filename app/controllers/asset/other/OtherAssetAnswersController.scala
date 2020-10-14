/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models.Status.Completed
import models.requests.RegistrationDataRequest
import models.{Mode, NormalMode}
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.other.{OtherAssetDescriptionPage, OtherAssetValuePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.CheckYourAnswersHelper
import utils.countryOptions.CountryOptions
import viewmodels.AnswerSection
import views.html.asset.other.OtherAssetAnswersView

import scala.concurrent.{ExecutionContext, Future}

class OtherAssetAnswersController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             repository: RegistrationsRepository,
                                             identify: RegistrationIdentifierAction,
                                             getData: DraftIdRetrievalActionProvider,
                                             requireData: RegistrationDataRequiredAction,
                                             requiredAnswer: RequiredAnswerActionProvider,
                                             view: OtherAssetAnswersView,
                                             countryOptions: CountryOptions,
                                             val controllerComponents: MessagesControllerComponents
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def actions(mode: Mode, index: Int, draftId: String): ActionBuilder[RegistrationDataRequest, AnyContent] = {
    identify andThen getData(draftId) andThen requireData andThen
      requiredAnswer(RequiredAnswer(WhatKindOfAssetPage(index), controllers.asset.routes.WhatKindOfAssetController.onPageLoad(mode, index, draftId))) andThen
      requiredAnswer(RequiredAnswer(OtherAssetDescriptionPage(index), routes.OtherAssetDescriptionController.onPageLoad(mode, index, draftId))) andThen
      requiredAnswer(RequiredAnswer(OtherAssetValuePage(index), routes.OtherAssetValueController.onPageLoad(mode, index, draftId)))
  }

  def onPageLoad(index: Int, draftId: String): Action[AnyContent] = actions(NormalMode, index, draftId) {
    implicit request =>

      val answers = new CheckYourAnswersHelper(countryOptions)(request.userAnswers, draftId, canEdit = true)
      val description = request.userAnswers.get(OtherAssetDescriptionPage(index)).get

      val sections = Seq(
        AnswerSection(
          None,
          Seq(
            answers.whatKindOfAsset(index),
            answers.otherAssetDescription(index),
            answers.otherAssetValue(index, description)
          ).flatten
        )
      )

      Ok(view(index, draftId, sections))
  }

  def onSubmit(index: Int, draftId: String): Action[AnyContent] = actions(NormalMode, index, draftId).async {
    implicit request =>

      val answers = request.userAnswers.set(AssetStatus(index), Completed)

      for {
        updatedAnswers <- Future.fromTry(answers)
        _ <- repository.set(updatedAnswers)
      } yield Redirect(controllers.asset.routes.AddAssetsController.onPageLoad(draftId))

  }
}