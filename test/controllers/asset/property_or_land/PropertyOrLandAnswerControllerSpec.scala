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

package controllers.asset.property_or_land

import base.SpecBase
import controllers.routes._
import models.Status.Completed
import models.{UKAddress, UserAnswers}
import models.WhatKindOfAsset.PropertyOrLand
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.property_or_land._
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.print.PropertyOrLandPrintHelper
import views.html.asset.property_or_land.PropertyOrLandAnswersView

class PropertyOrLandAnswerControllerSpec extends SpecBase {

  private val index: Int = 0

  private val description = "Property Land Description"
  private val totalValue  = 10000L
  private val ukAddress   = UKAddress("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), "AB1 1AB")

  private def propertyOrLandAssetWithDescription(
    userAnswers: UserAnswers,
    index: Int,
    description: String = description,
    totalValue: Long = totalValue,
    trustOwnAll: Boolean = true,
    trustValue: Option[Long] = None,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), PropertyOrLand)
      .success
      .value
      .set(PropertyOrLandAddressYesNoPage(index), false)
      .success
      .value
      .set(PropertyOrLandDescriptionPage(index), description)
      .success
      .value
      .set(PropertyOrLandTotalValuePage(index), totalValue)
      .success
      .value
      .set(TrustOwnAllThePropertyOrLandPage(index), trustOwnAll)
      .success
      .value

    val withTrustValue = trustValue match {
      case Some(v) => base.set(PropertyLandValueTrustPage(index), v).success.value
      case None    => base
    }

    if (completed) withTrustValue.set(AssetStatus(index), Completed).success.value else withTrustValue
  }

  private def propertyOrLandAssetWithAddress(
    userAnswers: UserAnswers,
    index: Int,
    address: UKAddress = ukAddress,
    totalValue: Long = totalValue,
    trustOwnAll: Boolean = true,
    trustValue: Option[Long] = None,
    completed: Boolean = false
  ): UserAnswers = {
    val base = userAnswers
      .set(WhatKindOfAssetPage(index), PropertyOrLand)
      .success
      .value
      .set(PropertyOrLandAddressYesNoPage(index), true)
      .success
      .value
      .set(PropertyOrLandAddressUkYesNoPage(index), true)
      .success
      .value
      .set(PropertyOrLandUKAddressPage(index), address)
      .success
      .value
      .set(PropertyOrLandTotalValuePage(index), totalValue)
      .success
      .value
      .set(TrustOwnAllThePropertyOrLandPage(index), trustOwnAll)
      .success
      .value

    val withTrustValue = trustValue match {
      case Some(v) => base.set(PropertyLandValueTrustPage(index), v).success.value
      case None    => base
    }

    if (completed) withTrustValue.set(AssetStatus(index), Completed).success.value else withTrustValue
  }

  private val baseAnswers: UserAnswers = propertyOrLandAssetWithDescription(emptyUserAnswers, index)

  private lazy val propertyOrLandAnswerRoute: String =
    routes.PropertyOrLandAnswerController.onPageLoad(index, fakeDraftId).url

  "PropertyOrLandAnswer Controller" must {

    "property or land does not have an address and total value is owned by the trust" must {

      "return OK and the correct view for a GET" in {

        val expectedSections                           = Nil
        val mockPrintHelper: PropertyOrLandPrintHelper = mock[PropertyOrLandPrintHelper]
        when(mockPrintHelper.checkDetailsSection(any(), any(), any(), any())(any())).thenReturn(Nil)

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PropertyOrLandPrintHelper].toInstance(mockPrintHelper))
          .build()

        val request = FakeRequest(GET, propertyOrLandAnswerRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PropertyOrLandAnswersView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(index, fakeDraftId, expectedSections)(request, messages).toString

        application.stop()
      }

    }

    "redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(
        userAnswers = Some(baseAnswers),
        navigator =
          new FakeNavigator(Call("GET", controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url))
      ).build()

      val request = FakeRequest(POST, propertyOrLandAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.AddAssetsController.onPageLoad(fakeDraftId).url

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, propertyOrLandAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(POST, propertyOrLandAnswerRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "not add a duplicate asset with description and redirect to duplicate asset view" in {

      val answersWithDuplicate = propertyOrLandAssetWithDescription(
        propertyOrLandAssetWithDescription(emptyUserAnswers, index = 0, completed = true),
        index = 1
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.PropertyOrLandAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

    "not add a duplicate asset with address and redirect to duplicate asset view" in {

      val answersWithDuplicate = propertyOrLandAssetWithAddress(
        propertyOrLandAssetWithAddress(emptyUserAnswers, index = 0, completed = true),
        index = 1
      )

      val application = applicationBuilder(userAnswers = Some(answersWithDuplicate)).build()

      val request = FakeRequest(POST, routes.PropertyOrLandAnswerController.onSubmit(1, fakeDraftId).url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual controllers.asset.routes.DuplicateAssetController
        .onPageLoad(fakeDraftId)
        .url

      application.stop()
    }

  }
}
