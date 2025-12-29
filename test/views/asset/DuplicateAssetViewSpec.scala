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

package views.asset

import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.asset.DuplicateAssetView

class DuplicateAssetViewSpec extends ViewBehaviours {

  private val view: DuplicateAssetView = viewFor[DuplicateAssetView](Some(emptyUserAnswers))

  private val applyView: HtmlFormat.Appendable = view(draftId)(fakeRequest, messages)

  "DuplicateAsset view" must {

    behave like normalPage(applyView, "duplicateAsset")

    behave like pageWithGuidance(applyView, "duplicateAsset", "p1")

    "contain a link to the add assets page" in {

      val doc  = asDocument(applyView)
      val link = doc.select("a[href*=add]").first()

      link must not be null
      link.attr("href") mustBe controllers.asset.routes.AddAssetsController.onPageLoad(draftId).url
      link.text() mustBe messages("duplicateAsset.link")
    }

    "not display a back link" in {

      val doc = asDocument(applyView)
      assertNotRenderedById(doc, "back-link")
    }

  }
}
