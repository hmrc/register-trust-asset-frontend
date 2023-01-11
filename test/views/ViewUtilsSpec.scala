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

package views

import base.SpecBase
import models.requests.RegistrationDataRequest
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.Enrolments

class ViewUtilsSpec extends SpecBase {

  "ViewUtils" when {

    lazy val viewUtils = injector.instanceOf[ViewUtils]

    ".breadcrumbTitle" must {

      lazy val fakeTitle = "Title"

      "return Assets" when {
        "registering a taxable trust" when {

          lazy val dataRequest = RegistrationDataRequest(fakeRequest, "", "", emptyUserAnswers.copy(isTaxable = true), Organisation, Enrolments(Set()))

          "Request" in {
            implicit val request: Request[_] = fakeRequest
            val result = viewUtils.breadcrumbTitle(fakeTitle)(request, messages)
            result mustBe s"$fakeTitle - Register and Maintain a Trust - GOV.UK"
          }

          "RegistrationDataRequest" in {
            implicit val request: RegistrationDataRequest[_] = dataRequest
            val result = viewUtils.breadcrumbTitle(fakeTitle)(request, messages)
            result mustBe s"$fakeTitle - Assets - Register and Maintain a Trust - GOV.UK"
          }
        }
      }

      "return Non-EEA" when {
        "registering a non-taxable trust" when {

          lazy val dataRequest = RegistrationDataRequest(fakeRequest, "", "", emptyUserAnswers.copy(isTaxable = false), Organisation, Enrolments(Set()))

          "Request" in {
            implicit val request: Request[_] = fakeRequest
            val result = viewUtils.breadcrumbTitle(fakeTitle)(request, messages)
            result mustBe s"$fakeTitle - Register and Maintain a Trust - GOV.UK"
          }

          "RegistrationDataRequest" in {
            implicit val request: RegistrationDataRequest[_] = dataRequest
            val result = viewUtils.breadcrumbTitle(fakeTitle)(request, messages)
            result mustBe s"$fakeTitle - Non-EEA - Register and Maintain a Trust - GOV.UK"
          }
        }
      }
    }
  }

}
