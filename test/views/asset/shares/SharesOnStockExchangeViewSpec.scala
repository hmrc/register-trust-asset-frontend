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

package views.asset.shares

import forms.YesNoFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.YesNoViewBehaviours
import views.html.asset.shares.SharesOnStockExchangeView

class SharesOnStockExchangeViewSpec extends YesNoViewBehaviours {

  private val messageKeyPrefix: String = "shares.onStockExchangeYesNo"
  private val index: Int               = 0
  private val companyName: String      = "Company"

  override val form: Form[Boolean] = new YesNoFormProvider().withPrefix(messageKeyPrefix)

  "SharesOnStockExchange view" must {

    val view = viewFor[SharesOnStockExchangeView](Some(emptyUserAnswers))

    def applyView(form: Form[_]): HtmlFormat.Appendable =
      view.apply(form, fakeDraftId, index, companyName)(fakeRequest, messages)

    behave like dynamicTitlePage(applyView(form), messageKeyPrefix, companyName, "hint")

    behave like pageWithBackLink(applyView(form))

    behave like yesNoPage(form, applyView, messageKeyPrefix, Seq(companyName))

    behave like pageWithASubmitButton(applyView(form))

  }
}
