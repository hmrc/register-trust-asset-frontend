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

package repositories

import mapping.AssetMapper
import models.{RegistrationSubmission, UserAnswers}
import pages.RegistrationProgress
import play.api.i18n.Messages
import play.api.libs.json.Json
import utils.answers._
import viewmodels.{AnswerRow, AnswerSection}

import javax.inject.Inject

class SubmissionSetFactory @Inject()(registrationProgress: RegistrationProgress,
                                     assetMapper: AssetMapper,
                                     moneyAnswersHelper: MoneyAnswersHelper,
                                     propertyOrLandAnswersHelper: PropertyOrLandAnswersHelper,
                                     sharesAnswersHelper: SharesAnswersHelper,
                                     businessAnswersHelper: BusinessAnswersHelper,
                                     partnershipAnswersHelper: PartnershipAnswersHelper,
                                     otherAnswersHelper: OtherAnswersHelper,
                                     nonEeaBusinessAnswersHelper: NonEeaBusinessAnswersHelper) {

  def createFrom(userAnswers: UserAnswers)(implicit messages: Messages): RegistrationSubmission.DataSet = {

    RegistrationSubmission.DataSet(
      data = Json.toJson(userAnswers),
      registrationPieces = mappedData(userAnswers),
      answerSections = answerSections(userAnswers)
    )
  }

  private def mappedData(userAnswers: UserAnswers): List[RegistrationSubmission.MappedPiece] = {
      assetMapper.build(userAnswers).map {
        assets =>
          RegistrationSubmission.MappedPiece("trust/assets", Json.toJson(assets))
      }.toList
  }

  def answerSections(userAnswers: UserAnswers)
                    (implicit messages: Messages): List[RegistrationSubmission.AnswerSection] = {

      val entitySections: List[AnswerSection] = List(
        moneyAnswersHelper(userAnswers),
        propertyOrLandAnswersHelper(userAnswers),
        sharesAnswersHelper(userAnswers),
        businessAnswersHelper(userAnswers),
        partnershipAnswersHelper(userAnswers),
        otherAnswersHelper(userAnswers),
        nonEeaBusinessAnswersHelper(userAnswers)
      ).flatten

      entitySections match {
        case Nil =>
          List.empty
        case _ =>
          val section = if (userAnswers.isTaxable) "assets" else "companyOwnershipOrControllingInterest"

          val updatedFirstSection: AnswerSection =
            entitySections.head.copy(sectionKey = Some(s"answerPage.section.$section.heading"))

          val updatedSections: List[AnswerSection] =
            updatedFirstSection :: entitySections.tail

          updatedSections.map(convertForSubmission)
      }
  }

  private def convertForSubmission(section: AnswerSection): RegistrationSubmission.AnswerSection = {
    RegistrationSubmission.AnswerSection(
      headingKey = section.headingKey,
      rows = section.rows.map(convertForSubmission),
      sectionKey = section.sectionKey,
      headingArgs = section.headingArgs.map(_.toString)
    )
  }

  private def convertForSubmission(row: AnswerRow): RegistrationSubmission.AnswerRow = {
    RegistrationSubmission.AnswerRow(row.label, row.answer.toString, row.labelArg)
  }
}
