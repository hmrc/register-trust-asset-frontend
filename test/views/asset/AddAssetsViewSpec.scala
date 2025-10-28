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

import forms.AddAssetsFormProvider
import models.{AddAssets, WhatKindOfAsset}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import viewmodels.AddRow
import views.behaviours.{OptionsViewBehaviours, QuestionViewBehaviours, TabularDataViewBehaviours}
import views.html.asset.AddAssetsView

class AddAssetsViewSpec extends OptionsViewBehaviours with TabularDataViewBehaviours with QuestionViewBehaviours[AddAssets] {


  private val view: AddAssetsView   = viewFor[AddAssetsView](Some(emptyUserAnswers))

  private val taxableAddAssetsPrefix: String    = "addAssets"
  private val nonTaxableAddAssetsPrefix: String = "addAssets.nonTaxable"

  val form: Form[AddAssets] = new AddAssetsFormProvider().withPrefix(nonTaxableAddAssetsPrefix)

  private def deriveTitleFromPrefix(count: Int, prefix: String): String =
    if (prefix == taxableAddAssetsPrefix) {
      if (count > 1) {
        s"You have added $count assets"
      } else {
        "Add assets"
      }
    } else {
      messages(s"$prefix.title")
    }
  
  private def applyView(form: Form[_], prefix: String): HtmlFormat.Appendable = {
    val title = if (prefix == taxableAddAssetsPrefix) {
      "Add assets"
    } else {
      messages(s"$prefix.title")
    }

    view.apply(form, fakeDraftId, Nil, Nil, title, prefix, Nil)(fakeRequest, messages)
  }

  private def applyView(
    form: Form[_],
    inProgressAssets: Seq[AddRow],
    completeAssets: Seq[AddRow],
    count: Int,
    prefix: String
  ): HtmlFormat.Appendable = {

    val title = deriveTitleFromPrefix(count, prefix)

    view.apply(form, fakeDraftId, inProgressAssets, completeAssets, title, prefix, Nil)(
      fakeRequest,
      messages
    )
  }

  def applyView(
    form: Form[_],
    completeAssets: Seq[AddRow],
    count: Int,
    maxedOut: List[(String, Int)],
    prefix: String
  ): HtmlFormat.Appendable = {
    val title = deriveTitleFromPrefix(count, prefix)
    view.apply(form, fakeDraftId, Nil, completeAssets, title, prefix, maxedOut)(fakeRequest, messages)
  }

  "Taxable AddAssetsView" when {

    val completeAssets: Seq[AddRow] = Seq(
      AddRow("4500", WhatKindOfAsset.Money.toString, "#", "#"),
      AddRow("4500", WhatKindOfAsset.Money.toString, "#", "#")
    )

    val inProgressAssets: Seq[AddRow] = Seq(
      AddRow("Tesco", WhatKindOfAsset.Shares.toString, "#", "#")
    )

    val taxableCreateViewFn = (f: Form[_]) => applyView(f, taxableAddAssetsPrefix)

    "there are no assets" must {
      behave like normalPage(applyView(form, taxableAddAssetsPrefix), taxableAddAssetsPrefix)

      behave like pageWithNoTabularData(applyView(form, taxableAddAssetsPrefix))

      behave like pageWithBackLink(applyView(form, taxableAddAssetsPrefix))

      behave like pageWithOptions(form, taxableCreateViewFn, AddAssets.options(taxableAddAssetsPrefix))
    }

    "there is data in progress" must {

      val viewWithData = applyView(form, inProgressAssets, Nil, 1, taxableAddAssetsPrefix)

      behave like dynamicTitlePage(viewWithData, s"$taxableAddAssetsPrefix", "1")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithInProgressTabularData(viewWithData, inProgressAssets)

      behave like pageWithOptions(form, taxableCreateViewFn, AddAssets.options(taxableAddAssetsPrefix))
    }

    "there is complete data" must {

      val viewWithData = applyView(form, Nil, completeAssets, 2, taxableAddAssetsPrefix)

      behave like dynamicTitlePage(viewWithData, s"$taxableAddAssetsPrefix.count", "2")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, taxableCreateViewFn, AddAssets.options(taxableAddAssetsPrefix))
    }

    "there is one maxed out asset type" must {

      val viewWithData = applyView(form, completeAssets, 10, List(("Partnership", 10)), taxableAddAssetsPrefix)

      behave like dynamicTitlePage(viewWithData, s"$taxableAddAssetsPrefix.count", "10")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, taxableCreateViewFn, AddAssets.options(taxableAddAssetsPrefix))

      "render content" in {
        val doc = asDocument(viewWithData)

        assertContainsText(doc, "You cannot add another partnership asset as you have entered a maximum of 10.")
        assertContainsText(
          doc,
          "Check the assets you have added. If you have further assets to add within this type, write to HMRC with their details."
        )
      }
    }

    "there is more than one maxed out asset type" must {

      val viewWithData = applyView(form, completeAssets, 11, List(("Money", 1), ("Partnership", 10)), taxableAddAssetsPrefix)

      behave like dynamicTitlePage(viewWithData, s"$taxableAddAssetsPrefix.count", "11")

      behave like pageWithBackLink(viewWithData)

      behave like pageWithCompleteTabularData(viewWithData, completeAssets)

      behave like pageWithOptions(form, taxableCreateViewFn, AddAssets.options(taxableAddAssetsPrefix))

      "render content" in {
        val doc = asDocument(viewWithData)

        assertContainsText(doc, "You have entered the maximum number of assets for:")
        assertContainsText(
          doc,
          "Check the assets you have added. If you have further assets to add within these types, write to HMRC with their details."
        )
      }
    }

  }

  "Non-Taxable AddAssetsView" when {

    "1 company in progress" must {

      val inProgressAssets: Seq[AddRow] = Seq(
        AddRow("Little Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#")
      )

      val nonEeaView = applyView(form, inProgressAssets, Seq.empty[AddRow], 2, nonTaxableAddAssetsPrefix)

      behave like pageWithInProgressTabularData(nonEeaView, inProgressAssets)

      behave like pageWithTitle(nonEeaView, nonTaxableAddAssetsPrefix)

      behave like pageWithBackLink(nonEeaView)

      val taxableCreateViewFn = (f: Form[_]) => applyView(form, inProgressAssets, Seq.empty[AddRow], 2, nonTaxableAddAssetsPrefix)

      behave like pageWithSubHeadings(form, taxableCreateViewFn, List("In Progress", "Do you want to add another company?"))
    }


    "1 company added and 1 in progress" must {

      val completeAssets: Seq[AddRow] = Seq(
        AddRow("Big Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#")
      )

      val inProgressAssets: Seq[AddRow] = Seq(
        AddRow("Little Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#")
      )

      val nonEeaView = applyView(form, inProgressAssets, completeAssets, 2, nonTaxableAddAssetsPrefix)

      behave like pageWithTabularData(nonEeaView, inProgressAssets, completeAssets)

      behave like pageWithTitle(nonEeaView, nonTaxableAddAssetsPrefix)

      behave like pageWithBackLink(nonEeaView)

      val taxableCreateViewFn = (f: Form[_]) => applyView(form, inProgressAssets, completeAssets, 2, nonTaxableAddAssetsPrefix)

      behave like pageWithSubHeadings(form, taxableCreateViewFn, List("In Progress", "You have added 1 company", "Do you want to add another company?"))
    }

    "2 companies added and 1 in progress" must {

      val completeAssets: Seq[AddRow] = Seq(
        AddRow("Big Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#"),
        AddRow("Medium Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#")
      )

      val inProgressAssets: Seq[AddRow] = Seq(
        AddRow("Little Company", WhatKindOfAsset.NonEeaBusiness.toString, "#", "#")
      )

      val nonEeaView = applyView(form, inProgressAssets, completeAssets, 2, nonTaxableAddAssetsPrefix)

      behave like pageWithTabularData(nonEeaView, inProgressAssets, completeAssets)

      behave like pageWithTitle(nonEeaView, nonTaxableAddAssetsPrefix)

      behave like pageWithBackLink(nonEeaView)

      val taxableCreateViewFn = (f: Form[_]) => applyView(form, inProgressAssets, completeAssets, 2, nonTaxableAddAssetsPrefix)

      behave like pageWithSubHeadings(form, taxableCreateViewFn, List("In Progress", "You have added 2 companies", "Do you want to add another company?"))
    }

  }
}
