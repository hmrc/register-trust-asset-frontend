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

package navigation

import models.UserAnswers
import pages.{Page, QuestionPage}
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.AffinityGroup

trait Navigator {

  def nextPage(page: Page, draftId: String, af: AffinityGroup = AffinityGroup.Organisation): UserAnswers => Call =
    route(draftId)(page)(af)

  protected def route(draftId: String): PartialFunction[Page, AffinityGroup => UserAnswers => Call] =
    defaultRoute(draftId)

  private def defaultRoute(draftId: String): PartialFunction[Page, AffinityGroup => UserAnswers => Call] = { case _ =>
    _ => _ => controllers.routes.IndexController.onPageLoad(draftId)
  }

  def yesNoNav(ua: UserAnswers, fromPage: QuestionPage[Boolean], yesCall: => Call, noCall: => Call): Call =
    ua.get(fromPage)
      .map(if (_) yesCall else noCall)
      .getOrElse(controllers.routes.SessionExpiredController.onPageLoad)
}
