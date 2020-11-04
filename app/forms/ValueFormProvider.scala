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

package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class ValueFormProvider @Inject() extends Mappings {

  def withConfig(prefix: String, maxValue: Option[String] = None): Form[String] = {

    maxValue match {
      case Some(value) =>
        Form(
          "value" -> currency(s"$prefix.error.required", s"$prefix.error.invalid")
            .verifying(
              firstError(
                isNotEmpty("value", s"$prefix.error.required"),
                regexp(Validation.decimalCheck, s"$prefix.error.whole"),
                regexp(Validation.onlyNumbersRegex, s"$prefix.error.invalid"),
                isLessThan(value, s"$prefix.error.moreThanTotal"),
                minimumValue("1", s"$prefix.error.zero")
              )
            )
        )
      case _ =>
        Form(
          "value"-> currency(s"$prefix.error.required", s"$prefix.error.invalid")
            .verifying(
              firstError(
                isNotEmpty("value", s"$prefix.error.required"),
                maxLength(12, s"$prefix.error.length"),
                regexp(Validation.decimalCheck, s"$prefix.error.whole"),
                regexp(Validation.onlyNumbersRegex, s"$prefix.error.invalid"),
                minimumValue("1", s"$prefix.error.zero")
              )
            )
        )
    }
  }
}
