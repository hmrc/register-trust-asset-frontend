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

import base.SpecBase
import controllers.asset.partnership.routes._
import controllers.asset.routes.WhatKindOfAssetController
import models.UserAnswers
import models.WhatKindOfAsset.Partnership
import pages.asset.WhatKindOfAssetPage
import pages.asset.partnership._
import play.twirl.api.Html
import viewmodels.{AnswerRow, AnswerSection}

import java.time.LocalDate

class PartnershipPrintHelperSpec extends SpecBase {

  private val helper: PartnershipPrintHelper = injector.instanceOf[PartnershipPrintHelper]

  private val index: Int = 0

  private val heading: String       = "answerPage.section.partnershipAsset.subheading"
  private val headingArgs: Seq[Any] = Seq(index + 1)

  private val description: String = "Description"
  private val date: LocalDate     = LocalDate.parse("1996-02-03")

  private val answers: UserAnswers = emptyUserAnswers
    .set(WhatKindOfAssetPage(index), Partnership)
    .success
    .value
    .set(PartnershipDescriptionPage(index), description)
    .success
    .value
    .set(PartnershipStartDatePage(index), date)
    .success
    .value

  private val rows: Seq[AnswerRow] = Seq(
    AnswerRow(
      "whatKindOfAsset.first.checkYourAnswersLabel",
      Html("Partnership"),
      Some(WhatKindOfAssetController.onPageLoad(index, fakeDraftId).url)
    ),
    AnswerRow(
      "partnership.description.checkYourAnswersLabel",
      Html(description),
      Some(PartnershipDescriptionController.onPageLoad(index, fakeDraftId).url)
    ),
    AnswerRow(
      "partnership.startDate.checkYourAnswersLabel",
      Html("3 February 1996"),
      Some(PartnershipStartDateController.onPageLoad(index, fakeDraftId).url)
    )
  )

  "PartnershipPrintHelper" when {

    "printSection" must {
      "render answer section with heading" in {

        val result: AnswerSection = helper.printSection(
          userAnswers = answers,
          index = index,
          specificIndex = index,
          draftId = fakeDraftId
        )

        result mustBe AnswerSection(
          headingKey = Some(heading),
          rows = rows,
          headingArgs = headingArgs
        )
      }
    }

    "checkDetailsSection" must {
      "render answer section without heading" in {

        val result: Seq[AnswerSection] = helper.checkDetailsSection(
          userAnswers = answers,
          index = index,
          draftId = fakeDraftId
        )

        result mustBe Seq(
          AnswerSection(
            headingKey = None,
            rows = rows
          )
        )
      }
    }
  }

}
