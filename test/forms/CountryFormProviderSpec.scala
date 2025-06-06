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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.{Form, FormError}

class CountryFormProviderSpec extends StringFieldBehaviours {

  private val messagePrefix = "nonEeaBusiness.governingCountry"
  private val requiredKey   = s"$messagePrefix.error.required"
  private val lengthKey     = s"$messagePrefix.error.length"
  private val maxLength     = 100
  private val regexp        = "^[A-Za-z ,.()'-]*$"
  private val invalidKey    = s"$messagePrefix.error.invalidCharacters"

  private val form: Form[String] = new CountryFormProvider().withPrefix(messagePrefix)

  ".value" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like checkForMaxLengthAndInvalid(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength)),
      invalidError = FormError(fieldName, invalidKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithRegexpWithGenerator(
      form,
      fieldName,
      regexp = regexp,
      generator = stringsWithMaxLength(maxLength),
      error = FormError(fieldName, invalidKey, Seq(regexp))
    )
  }
}
