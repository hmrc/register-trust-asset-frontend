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

package views.asset.partnership

import forms.DescriptionFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.StringViewBehaviours
import views.html.asset.partnership.PartnershipDescriptionView

class PartnershipDescriptionViewSpec extends StringViewBehaviours {

  private val messageKeyPrefix: String = "partnership.description"

  override val form: Form[String] = new DescriptionFormProvider().withConfig(56, messageKeyPrefix)

  "PartnershipDescription view" must {

    val view = viewFor[PartnershipDescriptionView](Some(emptyUserAnswers))

    def applyView(form: Form[_]): HtmlFormat.Appendable =
      view.apply(form, 0, fakeDraftId)(fakeRequest, messages)

    behave like normalPage(applyView(form), messageKeyPrefix)

    behave like pageWithBackLink(applyView(form))

    behave like stringPage(form, applyView, messageKeyPrefix, None)

    behave like pageWithASubmitButton(applyView(form))
  }
}
