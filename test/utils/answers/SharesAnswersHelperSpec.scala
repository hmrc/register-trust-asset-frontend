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

package utils.answers

import base.SpecBase
import models.WhatKindOfAsset.Shares
import models.{ShareClass, UserAnswers}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import pages.asset.WhatKindOfAssetPage
import pages.asset.shares._
import utils.print.SharesPrintHelper
import viewmodels.AnswerSection

class SharesAnswersHelperSpec extends SpecBase {

  private val mockPrintHelper: SharesPrintHelper = mock[SharesPrintHelper]
  private val answersHelper: SharesAnswersHelper = new SharesAnswersHelper(mockPrintHelper)

  private val name: String = "Name"

  "SharesAnswersHelper" when {

    "there are no assets" must {
      "return Nil" in {

        val result: Seq[AnswerSection] = answersHelper(emptyUserAnswers, name)
        result mustBe Nil
      }
    }

    "there are assets" must {

      val userAnswers: UserAnswers = emptyUserAnswers
        .set(WhatKindOfAssetPage(0), Shares).success.value
        .set(SharesInAPortfolioPage(0), true).success.value
        .set(SharePortfolioNamePage(0), "Name").success.value
        .set(SharePortfolioOnStockExchangePage(0), true).success.value
        .set(SharePortfolioQuantityInTrustPage(0), "100").success.value
        .set(SharePortfolioValueInTrustPage(0), 100L).success.value

        .set(WhatKindOfAssetPage(1), Shares).success.value
        .set(SharesInAPortfolioPage(1), false).success.value
        .set(ShareCompanyNamePage(1), "Name").success.value
        .set(SharesOnStockExchangePage(1), true).success.value
        .set(ShareClassPage(1), ShareClass.Ordinary).success.value
        .set(ShareQuantityInTrustPage(1), "100").success.value
        .set(ShareValueInTrustPage(1), 100L).success.value

      "interact with SharesPrintHelper" in {

        reset(mockPrintHelper)

        when(mockPrintHelper.printSection(any(), any(), any(), any())(any())).thenReturn(AnswerSection())

        val result: Seq[AnswerSection] = answersHelper(userAnswers, name)

        result mustBe Seq(AnswerSection(), AnswerSection())

        verify(mockPrintHelper, times(2)).printSection(any(), any(), any(), any())(any())
      }
    }
  }
}