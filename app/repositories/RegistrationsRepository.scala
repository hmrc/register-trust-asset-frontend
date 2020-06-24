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

package repositories

import config.FrontendAppConfig
import connectors.SubmissionDraftConnector
import javax.inject.Inject
import models.UserAnswers
import play.api.http.Status
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json, __}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DefaultRegistrationsRepository @Inject()(submissionDraftConnector: SubmissionDraftConnector,
                                               config: FrontendAppConfig
                                        )(implicit ec: ExecutionContext) extends RegistrationsRepository {

  private val userAnswersSection = config.appName
  private val registrationSection = "registration"

  override def get(draftId: String)(implicit hc: HeaderCarrier): Future[Option[UserAnswers]] = {
    submissionDraftConnector.getDraftSection(draftId, userAnswersSection).map {
      response => Some(response.data.as[UserAnswers])
    }
  }

  override def set(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Boolean] = {
    setSectionData(userAnswers.draftId, userAnswersSection, Json.toJson(userAnswers))
  }

  private def setSectionData(draftId: String, section: String, jsonData: JsValue)(implicit hc: HeaderCarrier) = {
    submissionDraftConnector.setDraftSection(
      draftId,
      section,
      jsonData
    ).map {
      response => response.status == Status.OK
    }
  }

  override def setRegistrationSection(draftId: String, path: String, registrationSectionData: JsValue)
                                     (implicit hc: HeaderCarrier): Future[Boolean] = {

    val sectionPath = (JsPath \ path).json

    submissionDraftConnector.getDraftSection(draftId, registrationSection).flatMap {
      data =>
        val transform = __.json.update(sectionPath.prune andThen sectionPath.put(registrationSectionData))
        data.data.transform(transform) match {
          case JsSuccess(value, _) => setSectionData(draftId, registrationSection, value)
          case _ => Future.successful(false)
        }
    }
  }
}

trait RegistrationsRepository {
  def get(draftId: String)(implicit hc: HeaderCarrier): Future[Option[UserAnswers]]

  def set(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Boolean]

  def setRegistrationSection(draftId: String, path: String, data: JsValue)(implicit hc: HeaderCarrier): Future[Boolean]
}
