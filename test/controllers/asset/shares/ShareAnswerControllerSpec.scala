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

package controllers.asset.shares

import base.SpecBase
import models.Status.Completed
import models.WhatKindOfAsset.Shares
import models.{ShareClass, UserAnswers}
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.shares._
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.print.SharesPrintHelper
import views.html.asset.shares.ShareAnswersView

class ShareAnswerControllerSpec extends SpecBase {

  private val index: Int    = 0
  private val companyName   = "Company Name"
  private val portfolioName = "Portfolio Name"
  private val quantity      = 100L
  private val shareValue    = 1000L
  private val shareClass    = ShareClass.Ordinary

  private def nonPortfolioShareAsset(
    userAnswers: UserAnswers,
    index: Int,
    name: String = companyName,
    onStockExchange: Boolean = true,
    shareClass: ShareClass = shareClass,
    quantity: Long = quantity,
    shareValue: Long = shareValue,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), Shares)
      .success
      .value
      .set(SharesInAPortfolioPage(index), false)
      .success
      .value
      .set(ShareCompanyNamePage(index), name)
      .success
      .value
      .set(SharesOnStockExchangePage(index), onStockExchange)
      .success
      .value
      .set(ShareClassPage(index), shareClass)
      .success
      .value
      .set(ShareQuantityInTrustPage(index), quantity)
      .success
      .value
      .set(ShareValueInTrustPage(index), shareValue)
      .success
      .value

    if (completed) base.set(AssetStatus(index), Completed).success.value else base
  }

  private def portfolioShareAsset(
    userAnswers: UserAnswers,
    index: Int,
    name: String = portfolioName,
    onStockExchange: Boolean = true,
    quantity: Long = quantity,
    shareValue: Long = shareValue,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), Shares)
      .success
      .value
      .set(SharesInAPortfolioPage(index), true)
      .success
      .value
      .set(SharePortfolioNamePage(index), name)
      .success
      .value
      .set(SharePortfolioOnStockExchangePage(index), onStockExchange)
      .success
      .value
      .set(SharePortfolioQuantityInTrustPage(index), quantity)
      .success
      .value
      .set(SharePortfolioValueInTrustPage(index), shareValue)
      .success
      .value

    if (completed) base.set(AssetStatus(index), Completed).success.value else base
  }

  private lazy val shareAnswerRoute: String = routes.ShareAnswerController.onPageLoad(index, fakeDraftId).url

  "ShareAnswer Controller" must {

    "return OK and the correct view for a GET" when {

      "share company name" in {

        val userAnswers = nonPortfolioShareAsset(emptyUserAnswers, index)

        val expectedSections                   = Nil
        val mockPrintHelper: SharesPrintHelper = mock[SharesPrintHelper]()
        when(mockPrintHelper.checkDetailsSection(any(), eqTo(companyName), any(), any())(any())).thenReturn(Nil)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SharesPrintHelper].toInstance(mockPrintHelper))
          .build()

        val request = FakeRequest(GET, shareAnswerRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ShareAnswersView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(index, fakeDraftId, expectedSections)(request, messages).toString

        application.stop()
      }

      "portfolio name" in {

        val userAnswers = portfolioShareAsset(emptyUserAnswers, index)

        val expectedSections                   = Nil
        val mockPrintHelper: SharesPrintHelper = mock[SharesPrintHelper]()
        when(mockPrintHelper.checkDetailsSection(any(), eqTo(portfolioName), any(), any())(any())).thenReturn(Nil)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SharesPrintHelper].toInstance(mockPrintHelper))
          .build()

        val request = FakeRequest(GET, shareAnswerRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ShareAnswersView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(index, fakeDraftId, expectedSections)(request, messages).toString

        application.stop()
      }

      "no name" in {

        val userAnswers = emptyUserAnswers
          .set(SharesInAPortfolioPage(index), true)
          .success
          .value

        val expectedSections                   = Nil
        val mockPrintHelper: SharesPrintHelper = mock[SharesPrintHelper]()
        when(mockPrintHelper.checkDetailsSection(any(), eqTo("the asset"), any(), any())(any())).thenReturn(Nil)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SharesPrintHelper].toInstance(mockPrintHelper))
          .build()

        val request = FakeRequest(GET, shareAnswerRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ShareAnswersView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(index, fakeDraftId, expectedSections)(request, messages).toString

        application.stop()
      }
    }

    "redirect to the next page when valid data is submitted" in {

      val userAnswers = nonPortfolioShareAsset(emptyUserAnswers, index)

      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        navigator =
          new FakeNavigator(Call("GET", controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url))
      ).build()

      val request = FakeRequest(POST, shareAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, shareAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "not add a duplicate non-portfolio share asset and redirect to duplicate asset view" in {

      val answersWithDuplicate = nonPortfolioShareAsset(
        nonPortfolioShareAsset(emptyUserAnswers, index = 0, completed = true),
        index = 1
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.ShareAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

    "detect duplicate regardless of case (for name)" in {

      val answersWithDuplicate = nonPortfolioShareAsset(
        nonPortfolioShareAsset(emptyUserAnswers, index = 0, name = "company name", completed = true),
        index = 1,
        name = "COMPANY NAME"
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.ShareAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

  }

}
