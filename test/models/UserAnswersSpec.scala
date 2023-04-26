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

package models

import base.SpecBase

class UserAnswersSpec extends SpecBase {

  "UserAnswers" when {

    "assets" must {

      "return correct information" when {

        "taxable" in {
          val userAnswers = emptyUserAnswers.copy(isTaxable = true)
          userAnswers.assets mustBe AssetViewModels(
            monetary = Some(Nil),
            propertyOrLand = Some(Nil),
            shares = Some(Nil),
            business = Some(Nil),
            partnerShip = Some(Nil),
            other = Some(Nil),
            nonEEABusiness = Some(Nil)
          )
        }

        "non-taxable" in {
          val userAnswers = emptyUserAnswers.copy(isTaxable = false)
          userAnswers.assets mustBe AssetViewModels(
            monetary = None,
            propertyOrLand = None,
            shares = None,
            business = None,
            partnerShip = None,
            other = None,
            nonEEABusiness = Some(Nil)
          )
        }

      }
    }
  }

}
