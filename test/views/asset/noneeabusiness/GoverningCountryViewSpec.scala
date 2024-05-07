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

package views.asset.noneeabusiness

import forms.CountryFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.InputOption
import utils.countryOptions.CountryOptions
import views.behaviours.SelectCountryViewBehaviours
import views.html.asset.noneeabusiness.GoverningCountryView

class GoverningCountryViewSpec extends SelectCountryViewBehaviours {

  private val messageKeyPrefix: String = "nonEeaBusiness.governingCountry"
  private val index: Int               = 0
  private val name: String             = "Test"

  override val form: Form[String] = new CountryFormProvider().withPrefix(messageKeyPrefix)

  private val countryOptions: Seq[InputOption] = injector.instanceOf[CountryOptions].options()

  "GoverningCountryView" must {

    val view = viewFor[GoverningCountryView](Some(emptyUserAnswers))

    def applyView(form: Form[_]): HtmlFormat.Appendable =
      view.apply(form, countryOptions, fakeDraftId, index, name)(fakeRequest, messages)

    behave like dynamicTitlePage(applyView(form), messageKeyPrefix, name)

    behave like pageWithBackLink(applyView(form))

    behave like selectCountryPage(form, applyView, messageKeyPrefix, name)

    behave like pageWithASubmitButton(applyView(form))
  }
}
