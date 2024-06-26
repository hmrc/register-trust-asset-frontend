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

package utils.print

import controllers.asset.shares.routes._
import models.UserAnswers
import pages.asset.shares._
import play.api.i18n.Messages
import utils.{AnswerRowConverter, CheckAnswersFormatters}
import viewmodels.AnswerRow

import javax.inject.Inject

class SharesPrintHelper @Inject() (checkAnswersFormatters: CheckAnswersFormatters) extends PrintHelper {

  override val assetType: String = "shareAsset"

  override def answerRows(userAnswers: UserAnswers, arg: String, index: Int, draftId: String)(implicit
    messages: Messages
  ): Seq[AnswerRow] = {

    val converter: AnswerRowConverter = new AnswerRowConverter(checkAnswersFormatters)(userAnswers, arg)

    Seq(
      converter.assetTypeQuestion(index, draftId),
      converter.yesNoQuestion(
        SharesInAPortfolioPage(index),
        "shares.inAPortfolioYesNo",
        SharesInAPortfolioController.onPageLoad(index, draftId).url
      ),
      converter.stringQuestion(
        ShareCompanyNamePage(index),
        "shares.companyName",
        ShareCompanyNameController.onPageLoad(index, draftId).url
      ),
      converter.stringQuestion(
        SharePortfolioNamePage(index),
        "shares.portfolioName",
        SharePortfolioNameController.onPageLoad(index, draftId).url
      ),
      converter.yesNoQuestion(
        SharesOnStockExchangePage(index),
        "shares.onStockExchangeYesNo",
        SharesOnStockExchangeController.onPageLoad(index, draftId).url
      ),
      converter.yesNoQuestion(
        SharePortfolioOnStockExchangePage(index),
        "shares.portfolioOnStockExchangeYesNo",
        SharePortfolioOnStockExchangeController.onPageLoad(index, draftId).url
      ),
      converter
        .shareClassQuestion(ShareClassPage(index), "shares.class", ShareClassController.onPageLoad(index, draftId).url),
      converter.numberQuestion(
        ShareQuantityInTrustPage(index),
        "shares.quantityInTrust",
        ShareQuantityInTrustController.onPageLoad(index, draftId).url
      ),
      converter.numberQuestion(
        SharePortfolioQuantityInTrustPage(index),
        "shares.portfolioQuantityInTrust",
        SharePortfolioQuantityInTrustController.onPageLoad(index, draftId).url
      ),
      converter.currencyQuestion(
        ShareValueInTrustPage(index),
        "shares.valueInTrust",
        ShareValueInTrustController.onPageLoad(index, draftId).url
      ),
      converter.currencyQuestion(
        SharePortfolioValueInTrustPage(index),
        "shares.portfolioValueInTrust",
        SharePortfolioValueInTrustController.onPageLoad(index, draftId).url
      )
    ).flatten
  }
}
