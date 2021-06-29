/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.AddAssetsFormProvider
import models.{AddAssets, WhatKindOfAsset}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import viewmodels.AddRow
import views.behaviours.{OptionsViewBehaviours, TabularDataViewBehaviours}
import views.html.asset.AddAssetsView

class AddAssetsViewSpec extends OptionsViewBehaviours with TabularDataViewBehaviours {

  private val completeAssets: Seq[AddRow] = Seq(
    AddRow("4500", WhatKindOfAsset.Money.toString, "#", "#"),
    AddRow("4500", WhatKindOfAsset.Money.toString, "#", "#")
  )

  private val inProgressAssets: Seq[AddRow] = Seq(
    AddRow("Tesco", WhatKindOfAsset.Shares.toString, "#", "#")
  )

  private val messageKeyPrefix: String = "addAssets"

  private val form: Form[AddAssets] = new AddAssetsFormProvider().withPrefix(messageKeyPrefix)

  private val view: AddAssetsView = viewFor[AddAssetsView](Some(emptyUserAnswers))

  private def applyView(form: Form[_]): HtmlFormat.Appendable =
    view.apply(form, fakeDraftId, Nil, Nil, "Add assets", messageKeyPrefix, Nil)(fakeRequest, messages)

  private def applyView(form: Form[_], inProgressAssets: Seq[AddRow], completeAssets: Seq[AddRow], count: Int): HtmlFormat.Appendable = {
    val title = if (count > 1) s"You have added $count assets" else "Add assets"
    view.apply(form, fakeDraftId, inProgressAssets, completeAssets, title, messageKeyPrefix, Nil)(fakeRequest, messages)
  }

  def applyView(form: Form[_], completeAssets: Seq[AddRow], count: Int, maxedOut: List[(String, Int)]): HtmlFormat.Appendable = {
    val title = if (count > 1) s"You have added $count assets" else "Add assets"
    view.apply(form, fakeDraftId, Nil, completeAssets, title, messageKeyPrefix, maxedOut)(fakeRequest, messages)
  }

  "AddAssetsView" when {

    "there are no assets" must {
      behave like normalPage(applyView(form), messageKeyPrefix)

      behave like pageWithNoTabularData(applyView(form))

      behave like pageWithBackLink(applyView(form))

      behave like pageWithOptions(form, applyView, AddAssets.options(messageKeyPrefix))
    }

    "there is data in progress" must {

      val viewWithData = applyView(form, inProgressAssets, Nil, 1)

      behave like dynamicTitlePage(viewWithData, s"$messageKeyPrefix", "1")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithInProgressTabularData(viewWithData, inProgressAssets)

      behave like pageWithOptions(form, applyView, AddAssets.options(messageKeyPrefix))
    }

    "there is complete data" must {

      val viewWithData = applyView(form, Nil, completeAssets, 2)

      behave like dynamicTitlePage(viewWithData, s"$messageKeyPrefix.count", "2")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, applyView, AddAssets.options(messageKeyPrefix))
    }

    "there is one maxed out asset type" must {

      val viewWithData = applyView(form, completeAssets, 10, List(("Partnership", 10)))

      behave like dynamicTitlePage(viewWithData, s"$messageKeyPrefix.count", "10")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, applyView, AddAssets.options(messageKeyPrefix))

      "render content" in {
        val doc = asDocument(viewWithData)

        assertContainsText(doc, "You cannot add another partnership asset as you have entered a maximum of 10.")
        assertContainsText(doc, "Check the assets you have added. If you have further assets to add within this type, write to HMRC with their details.")
      }
    }

    "there is more than one maxed out asset type" must {

      val viewWithData = applyView(form, completeAssets, 11, List(("Money", 1), ("Partnership", 10)))

      behave like dynamicTitlePage(viewWithData, s"$messageKeyPrefix.count", "11")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, applyView, AddAssets.options(messageKeyPrefix))

      "render content" in {
        val doc = asDocument(viewWithData)

        assertContainsText(doc, "You have entered the maximum number of assets for:")
        assertContainsText(doc, "Check the assets you have added. If you have further assets to add within these types, write to HMRC with their details.")
      }
    }

  }
}
