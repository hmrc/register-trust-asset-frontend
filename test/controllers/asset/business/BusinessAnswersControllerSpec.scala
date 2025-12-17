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

package controllers.asset.business

import base.SpecBase
import controllers.routes._
import models.Status.Completed
import models.WhatKindOfAsset.Business
import models.{UKAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.business._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.print.BusinessPrintHelper
import views.html.asset.business.BusinessAnswersView

class BusinessAnswersControllerSpec extends SpecBase {

  private val index       = 0
  private val name        = "Business Name"
  private val description = "Business Description"
  private val ukAddress   = UKAddress("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), "AB1 1AB")
  private val totalValue  = 12L

  private def businessAsset(
    userAnswers: UserAnswers,
    index: Int,
    name: String = name,
    description: String = description,
    address: UKAddress = ukAddress,
    value: Long = totalValue,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), Business)
      .success
      .value
      .set(BusinessNamePage(index), name)
      .success
      .value
      .set(BusinessDescriptionPage(index), description)
      .success
      .value
      .set(BusinessAddressUkYesNoPage(index), true)
      .success
      .value
      .set(BusinessUkAddressPage(index), address)
      .success
      .value
      .set(BusinessValuePage(index), value)
      .success
      .value

    if (completed) base.set(AssetStatus(index), Completed).success.value else base
  }

  private val answers: UserAnswers = businessAsset(emptyUserAnswers, index)

  "AssetAnswerPage Controller" must {

    "return OK and the correct view for a GET" in {

      val expectedSections                     = Nil
      val mockPrintHelper: BusinessPrintHelper = mock[BusinessPrintHelper]()
      when(mockPrintHelper.checkDetailsSection(any(), any(), any(), any())(any())).thenReturn(Nil)

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[BusinessPrintHelper].toInstance(mockPrintHelper))
        .build()

      val request = FakeRequest(GET, routes.BusinessAnswersController.onPageLoad(index, fakeDraftId).url)

      val result = route(application, request).value

      val view = application.injector.instanceOf[BusinessAnswersView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(index, fakeDraftId, expectedSections)(request, messages).toString

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(index, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "redirect to AssetNamePage when valid data is submitted with no AssetName answer" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(index, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.BusinessNameController.onPageLoad(index, fakeDraftId).url

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, routes.BusinessAnswersController.onPageLoad(index, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(index, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "not add a duplicate asset and redirect to duplicate asset view" in {

      val answersWithDuplicate = businessAsset(
        businessAsset(emptyUserAnswers, index = 0, completed = true),
        index = 1
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

    "add asset when not a duplicate (different name)" in {

      val answersWithDifferentAsset = businessAsset(
        businessAsset(emptyUserAnswers, index = 0, completed = true),
        index = 1,
        name = "Different Name"
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDifferentAsset)).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "detect duplicate regardless of case (for name)" in {

      val answersWithDuplicate = businessAsset(
        businessAsset(emptyUserAnswers, index = 0, name = "business name", completed = true),
        index = 1,
        name = "BUSINESS NAME"
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.BusinessAnswersController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

  }
}
