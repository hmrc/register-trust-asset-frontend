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

package controllers.asset.partnership

import base.SpecBase
import models.Status.Completed
import models.UserAnswers
import models.WhatKindOfAsset.Partnership
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.partnership._
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.print.PartnershipPrintHelper
import views.html.asset.partnership.PartnershipAnswersView

import java.time.{LocalDate, ZoneOffset}

class PartnershipAnswerControllerSpec extends SpecBase {

  private val index                = 0
  private val description          = "Partnership Description"
  private val validDate: LocalDate = LocalDate.now(ZoneOffset.UTC)

  private def partnershipAsset(
    userAnswers: UserAnswers,
    index: Int,
    description: String = description,
    startDate: LocalDate = validDate,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), Partnership)
      .success
      .value
      .set(PartnershipDescriptionPage(index), description)
      .success
      .value
      .set(PartnershipStartDatePage(index), startDate)
      .success
      .value

    if (completed) base.set(AssetStatus(index), Completed).success.value else base
  }

  private val baseAnswers: UserAnswers = partnershipAsset(emptyUserAnswers, index)

  private lazy val partnershipAnswerRoute: String =
    routes.PartnershipAnswerController.onPageLoad(index, fakeDraftId).url

  "PartnershipAnswer Controller" must {

    "return OK and the correct view for a GET" in {

      val expectedSections                        = Nil
      val mockPrintHelper: PartnershipPrintHelper = mock[PartnershipPrintHelper]()
      when(mockPrintHelper.checkDetailsSection(any(), any(), any(), any())(any())).thenReturn(Nil)

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[PartnershipPrintHelper].toInstance(mockPrintHelper))
        .build()

      val request = FakeRequest(GET, partnershipAnswerRoute)

      val result = route(application, request).value

      val view = application.injector.instanceOf[PartnershipAnswersView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(index, fakeDraftId, expectedSections)(request, messages).toString

      application.stop()
    }

    "redirect to PartnershipDescription page on a GET if no answer for 'What is the description for the partnership?' at index" in {

      val answers = emptyUserAnswers
        .set(WhatKindOfAssetPage(index), Partnership)
        .success
        .value
        .set(PartnershipStartDatePage(index), validDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      val request = FakeRequest(GET, partnershipAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.PartnershipDescriptionController
        .onPageLoad(index, fakeDraftId)
        .url

      application.stop()
    }

    "redirect to PartnershipStartDate page on a GET if no answer for 'When did the partnership start?' at index" in {

      val answers = emptyUserAnswers
        .set(WhatKindOfAssetPage(index), Partnership)
        .success
        .value
        .set(PartnershipDescriptionPage(index), description)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      val request = FakeRequest(GET, partnershipAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.PartnershipStartDateController.onPageLoad(index, fakeDraftId).url

      application.stop()

    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, partnershipAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "not add a duplicate asset and redirect to AddAssets page" in {

      val answersWithDuplicate = partnershipAsset(
        partnershipAsset(emptyUserAnswers, index = 0, completed = true),
        index = 1
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.PartnershipAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "add asset when not a duplicate (different description)" in {

      val answersWithDifferentAsset = partnershipAsset(
        partnershipAsset(emptyUserAnswers, index = 0, completed = true),
        index = 1,
        description = "Different Description"
      )

      val application = applicationBuilder(
        userAnswers = Some(answersWithDifferentAsset),
        navigator =
          new FakeNavigator(Call("GET", controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url))
      ).build()

      val request = FakeRequest(POST, routes.PartnershipAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "detect duplicate regardless of case (for description)" in {

      val answersWithDuplicate = partnershipAsset(
        partnershipAsset(emptyUserAnswers, index = 0, description = "partnership description", completed = true),
        index = 1,
        description = "PARTNERSHIP DESCRIPTION"
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.PartnershipAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

  }
}
