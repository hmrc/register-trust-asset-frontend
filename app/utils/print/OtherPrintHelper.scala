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

package utils.print

import controllers.asset.other.routes._
import models.UserAnswers
import pages.asset.other._
import play.api.i18n.Messages
import utils.{AnswerRowConverter, CheckAnswersFormatters}
import viewmodels.AnswerRow

import javax.inject.Inject

class OtherPrintHelper @Inject() (checkAnswersFormatters: CheckAnswersFormatters) extends PrintHelper {

  override val assetType: String = "otherAsset"

  override def answerRows(userAnswers: UserAnswers, arg: String, index: Int, draftId: String)(implicit
    messages: Messages
  ): Seq[AnswerRow] = {

    val converter: AnswerRowConverter = new AnswerRowConverter(checkAnswersFormatters)(userAnswers, arg)

    Seq(
      converter.assetTypeQuestion(index, draftId),
      converter.stringQuestion(
        OtherAssetDescriptionPage(index),
        "other.description",
        OtherAssetDescriptionController.onPageLoad(index, draftId).url
      ),
      converter.currencyQuestion(
        OtherAssetValuePage(index),
        "other.value",
        OtherAssetValueController.onPageLoad(index, draftId).url
      )
    ).flatten
  }
}
