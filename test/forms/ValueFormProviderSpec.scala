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

import forms.behaviours.{IntFieldBehaviours, StringFieldBehaviours}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError
import wolfendale.scalacheck.regexp.RegexpGen

class ValueFormProviderSpec extends StringFieldBehaviours with IntFieldBehaviours {

  private val fieldName = "value"
  private val prefix: String = "propertyOrLand.valueInTrust"
  private val requiredKey = s"$prefix.error.required"
  private val zeroNumberkey = s"$prefix.error.zero"
  private val invalidOnlyNumbersKey = s"$prefix.error.invalid"
  private val invalidWholeNumberKey = s"$prefix.error.whole"

  ".value" should {

    "max value" must {

      val maxValueKey = s"$prefix.error.moreThanTotal"

      val maxValue: Int = 100

      val form = new ValueFormProvider().withConfig(prefix, Some(maxValue.toString))

      behave like intFieldWithMaxValue(
        form,
        fieldName,
        nonNumericError = FormError(fieldName, invalidOnlyNumbersKey, Seq(Validation.onlyNumbersRegex)),
        wholeNumberError = FormError(fieldName, invalidWholeNumberKey, Seq(Validation.decimalCheck))
      )

      behave like fieldWithRegexpWithGenerator(
        form,
        fieldName,
        Validation.onlyNumbersRegex,
        arbitrary[String],
        error = FormError(fieldName, invalidOnlyNumbersKey, Seq(Validation.onlyNumbersRegex))
      )

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        RegexpGen.from(Validation.onlyNumbersRegex)
      )

      behave like intFieldWithMaximum(
        form,
        fieldName,
        maxValue,
        FormError(fieldName, maxValueKey, Array("100"))
      )

      behave like intFieldWithMinimumWithGenerator(
        form,
        fieldName,
        1,
        Gen.const(0),
        FormError(fieldName, zeroNumberkey, Array("1"))
      )

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    }

    "no max value" must {

      val maxLengthKey = s"$prefix.error.length"

      val maxLength = 12

      val form = new ValueFormProvider().withConfig(prefix, None)

      behave like nonDecimalField(
        form,
        fieldName,
        wholeNumberError = FormError(fieldName, invalidWholeNumberKey, Seq(Validation.decimalCheck)),
        maxLength = Some(maxLength)
      )

      behave like fieldWithRegexpWithGenerator(
        form,
        fieldName,
        Validation.onlyNumbersRegex,
        stringsWithMaxLength(maxLength),
        error = FormError(fieldName, invalidOnlyNumbersKey, Seq(Validation.onlyNumbersRegex))
      )

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        RegexpGen.from(Validation.onlyNumbersRegex)
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength,
        lengthError = FormError(fieldName, maxLengthKey, Seq(maxLength))
      )

      behave like intFieldWithMinimumWithGenerator(
        form,
        fieldName,
        1,
        Gen.const(0),
        FormError(fieldName, zeroNumberkey, Array("1"))
      )

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    }
  }
}
