/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import base.SpecBase
import connectors.SubmissionDraftConnector
import controllers.asset.routes._
import models.Status.Completed
import models.{TaskStatus, UserAnswers, WhatKindOfAsset}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.scalatest.BeforeAndAfterEach
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.money.AssetMoneyValuePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.TrustsStoreService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val trustsStoreService: TrustsStoreService = mock[TrustsStoreService]
  private val submissionDraftConnector: SubmissionDraftConnector = mock[SubmissionDraftConnector]

  private lazy val onPageLoadRoute: String = routes.IndexController.onPageLoad(fakeDraftId).url

  override protected def beforeEach(): Unit = {
    reset(trustsStoreService)

    when(trustsStoreService.updateTaskStatus(any(), any())(any(), any()))
      .thenReturn(Future.successful(HttpResponse(OK, "")))
  }

  "Index Controller" when {

    "pre-existing user answers" must {

      "redirect to AddAssetsController" when {

        "existing assets" in {

          reset(registrationsRepository)

          val userAnswers: UserAnswers = emptyUserAnswers
            .set(WhatKindOfAssetPage(0), WhatKindOfAsset.Money).success.value
            .set(AssetMoneyValuePage(0), 100L).success.value
            .set(AssetStatus(0), Completed).success.value

          val application = applicationBuilder()
            .overrides(
              bind[TrustsStoreService].toInstance(trustsStoreService),
              bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
            ).build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(userAnswers)))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(false))

          val request = FakeRequest(GET, onPageLoadRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustBe AddAssetsController.onPageLoad(fakeDraftId).url

          verify(trustsStoreService, atLeastOnce).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

          application.stop()
        }
      }

      "redirect to AssetInterruptPageController" when {

        "no existing assets and set task to in progress" when {

          "taxable" in {

            reset(registrationsRepository)

            val application = applicationBuilder()
              .overrides(
                bind[TrustsStoreService].toInstance(trustsStoreService),
                bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
              ).build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(true))

            val request = FakeRequest(GET, onPageLoadRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustBe AssetInterruptPageController.onPageLoad(fakeDraftId).url

            verify(trustsStoreService, atLeastOnce).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }
        }
      }

      "redirect to TrustOwnsNonEeaBusinessYesNoController" when {
        "no existing assets and set task to in progress" when {
          "non-taxable" in {

            reset(registrationsRepository)

            val application = applicationBuilder()
              .overrides(
                bind[TrustsStoreService].toInstance(trustsStoreService),
                bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
              ).build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(false))

            val request = FakeRequest(GET, onPageLoadRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustBe TrustOwnsNonEeaBusinessYesNoController.onPageLoad(fakeDraftId).url

            verify(trustsStoreService, atLeastOnce).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }
        }
      }

      "update value of isTaxable in user answers" in {

        reset(registrationsRepository)

        val userAnswers = emptyUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TrustsStoreService].toInstance(trustsStoreService),
            bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
          ).build()

        when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(userAnswers)))
        when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
        when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, onPageLoadRoute)

        route(application, request).value.map { _ =>
          val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

          uaCaptor.getValue.isTaxable mustBe true

          application.stop()
        }
      }
    }

    "no pre-existing user answers" must {

      "redirect to AssetInterruptPageController and set task to in progress" when {
        "taxable" in {

          reset(registrationsRepository)

          val application = applicationBuilder()
            .overrides(
              bind[TrustsStoreService].toInstance(trustsStoreService),
              bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
            ).build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(true))

          val request = FakeRequest(GET, onPageLoadRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustBe AssetInterruptPageController.onPageLoad(fakeDraftId).url

          verify(trustsStoreService, atLeastOnce).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

          application.stop()
        }
      }

      "redirect to TrustOwnsNonEeaBusinessYesNoController and set task to in progress" when {
        "non-taxable" in {

          reset(registrationsRepository)

          val application = applicationBuilder()
            .overrides(
              bind[TrustsStoreService].toInstance(trustsStoreService),
              bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
            ).build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(false))

          val request = FakeRequest(GET, onPageLoadRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustBe TrustOwnsNonEeaBusinessYesNoController.onPageLoad(fakeDraftId).url

          verify(trustsStoreService, atLeastOnce).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

          application.stop()
        }
      }

      "instantiate new set of user answers" when {

          "taxable" must {
            "add isTaxable = true to user answers" in {

              reset(registrationsRepository)

              val application = applicationBuilder(userAnswers = None)
                .overrides(
                  bind[TrustsStoreService].toInstance(trustsStoreService),
                  bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
                ).build()

              when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(true))

              val request = FakeRequest(GET, onPageLoadRoute)

              route(application, request).value.map { _ =>
                val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
                verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

                uaCaptor.getValue.isTaxable mustBe true
                uaCaptor.getValue.draftId mustBe fakeDraftId
                uaCaptor.getValue.internalAuthId mustBe "internalId"

                application.stop()
              }
            }
          }

          "non-taxable" must {
            "add isTaxable = false to user answers" in {

              reset(registrationsRepository)

              val application = applicationBuilder(userAnswers = None)
                .overrides(
                  bind[TrustsStoreService].toInstance(trustsStoreService),
                  bind[SubmissionDraftConnector].toInstance(submissionDraftConnector)
                ).build()

              when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(false))

              val request = FakeRequest(GET, onPageLoadRoute)

              route(application, request).value.map { _ =>
                val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
                verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

                uaCaptor.getValue.isTaxable mustBe false
                uaCaptor.getValue.draftId mustBe fakeDraftId
                uaCaptor.getValue.internalAuthId mustBe "internalId"

                application.stop()
              }
            }
          }

      }
    }
  }
}
