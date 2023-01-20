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

package controllers.asset

import base.SpecBase
import forms.{AddAssetsFormProvider, YesNoFormProvider}
import generators.Generators
import models.AddAssets.{NoComplete, YesNow}
import models.Status.{Completed, InProgress}
import models.WhatKindOfAsset._
import models.{AddAssets, Status, TaskStatus, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.scalatest.BeforeAndAfterEach
import pages.asset.noneeabusiness.NamePage
import pages.asset.{AddAnAssetYesNoPage, AddAssetsPage, WhatKindOfAssetPage}
import pages.{AssetStatus, RegistrationProgress}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.TrustsStoreService
import uk.gov.hmrc.http.HttpResponse
import utils.AddAssetViewHelper
import viewmodels.AddRow
import views.html.asset.{AddAnAssetYesNoView, AddAssetsView, MaxedOutView}

import scala.concurrent.Future

class AddAssetsControllerSpec extends SpecBase with Generators with BeforeAndAfterEach {

  private lazy val addAssetsRoute: String = routes.AddAssetsController.onPageLoad(fakeDraftId).url
  private lazy val addOnePostRoute: String = routes.AddAssetsController.submitOne(fakeDraftId).url
  private lazy val addAnotherPostRoute: String = routes.AddAssetsController.submitAnother(fakeDraftId).url
  private lazy val completePostRoute: String = routes.AddAssetsController.submitComplete(fakeDraftId).url

  private def changeNonEeaAssetRoute(index: Int): String =
    noneeabusiness.routes.AnswersController.onPageLoad(index, fakeDraftId).url

  private def removeAssetYesNoRoute(index: Int): String =
    routes.RemoveAssetYesNoController.onPageLoad(index, fakeDraftId).url

  private val addTaxableAssetsForm: Form[AddAssets] = new AddAssetsFormProvider().withPrefix("addAssets")
  private val addNonTaxableAssetsForm: Form[AddAssets] = new AddAssetsFormProvider().withPrefix("addAssets.nonTaxable")
  private val yesNoForm: Form[Boolean] = new YesNoFormProvider().withPrefix("addAnAssetYesNo")

  private val mockTrustsStoreService = mock[TrustsStoreService]
  private val mockRegistrationProgress = mock[RegistrationProgress]

  private lazy val oneAsset: List[AddRow] = List(AddRow("Name", typeLabel = "Non-EEA Company", changeNonEeaAssetRoute(0), removeAssetYesNoRoute(0)))

  private lazy val multipleAssets: List[AddRow] = oneAsset :+ AddRow("Name", typeLabel = "Non-EEA Company", changeNonEeaAssetRoute(1), removeAssetYesNoRoute(1))

  private val userAnswersWithOneAsset: UserAnswers = emptyUserAnswers
    .set(WhatKindOfAssetPage(0), NonEeaBusiness).success.value
    .set(NamePage(0), "Name").success.value
    .set(AssetStatus(0), Completed).success.value

  private def userAnswersWithMultipleAssets(status: Status = Completed): UserAnswers = userAnswersWithOneAsset
    .set(WhatKindOfAssetPage(1), NonEeaBusiness).success.value
    .set(NamePage(1), "Name").success.value
    .set(AssetStatus(1), status).success.value

  override protected def beforeEach(): Unit = {
    reset(mockTrustsStoreService)

    when(mockTrustsStoreService.updateTaskStatus(any(), any())(any(), any()))
      .thenReturn(Future.successful(HttpResponse(OK, "")))
  }

  "AddAssets Controller" when {

    "no data" must {
      "redirect to Session Expired for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val request = FakeRequest(GET, addAssetsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad.url

        application.stop()
      }

      "redirect to Session Expired for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val request =
          FakeRequest(POST, addAssetsRoute)
            .withFormUrlEncodedBody(("value", AddAssets.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.SessionExpiredController.onPageLoad.url

        application.stop()
      }
    }

    "there are no assets" when {

      "taxable" must {
        "return OK and the correct view for a GET" in {

          val answers = emptyUserAnswers.copy(isTaxable = true)

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddAnAssetYesNoView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(addTaxableAssetsForm, fakeDraftId)(request, messages).toString

          application.stop()
        }
      }

      "non-taxable" must {
        "redirect to TrustOwnsNonEeaBusinessYesNoController" in {

          val answers = emptyUserAnswers.copy(isTaxable = false)

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.TrustOwnsNonEeaBusinessYesNoController.onPageLoad(fakeDraftId).url

          application.stop()
        }
      }

      "redirect to the next page when valid data is submitted" when {

        val indexOfNewAsset = 0

        "taxable" must {
          "set task to in progress when answer yes in AddAnAssetYesNoPage" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(isTaxable = true)))
                .overrides(
                  bind[TrustsStoreService].to(mockTrustsStoreService)
                )
                .build()

            val request = FakeRequest(POST, addOnePostRoute)
              .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

            uaCaptor.getValue.get(AddAnAssetYesNoPage).get mustBe true
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }

          "set task to in progress when answer no in AddAnAssetYesNoPage" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers =
                Some(emptyUserAnswers.copy(isTaxable = true))
              ).overrides(
                bind[TrustsStoreService].to(mockTrustsStoreService)
              ).build()

            val request = FakeRequest(POST, addOnePostRoute)
              .withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

            uaCaptor.getValue.get(AddAnAssetYesNoPage).get mustBe false
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }
        }

        "non-taxable" must {

          "set task to in progress and values when answers yes" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))

            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(isTaxable = false)))
                .overrides(
                  bind[TrustsStoreService].to(mockTrustsStoreService)
                ).build()

            val request = FakeRequest(POST, addOnePostRoute)
              .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAnAssetYesNoPage).get mustBe true
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)).get mustBe NonEeaBusiness

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }

          "set task to complete and set values when answers no" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))

            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(isTaxable = false)))
                .overrides(
                  bind[TrustsStoreService].to(mockTrustsStoreService)
                ).build()

            val request = FakeRequest(POST, addOnePostRoute)
              .withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAnAssetYesNoPage).get mustBe false
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)).get mustBe NonEeaBusiness

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.Completed))(any(), any())

            application.stop()
          }
        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val request =
          FakeRequest(POST, addOnePostRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = yesNoForm.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddAnAssetYesNoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual
          view(boundForm, fakeDraftId)(request, messages).toString

        application.stop()
      }
    }

    "there is one asset" must {

      "return OK and the correct view for a GET" when {

        "taxable" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersWithOneAsset.copy(isTaxable = true))).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddAssetsView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(addTaxableAssetsForm, fakeDraftId, Nil, oneAsset, "Add assets", "addAssets", Nil)(request, messages).toString

          application.stop()
        }

        "non-taxable" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersWithOneAsset.copy(isTaxable = false))).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddAssetsView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(addNonTaxableAssetsForm, fakeDraftId, Nil, oneAsset, "Add a non-EEA company", "addAssets.nonTaxable", Nil)(request, messages).toString

          application.stop()
        }
      }
    }

    "there are existing assets" must {

      "return OK and the correct view for a GET" when {

        "taxable" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true))).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddAssetsView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(addTaxableAssetsForm, fakeDraftId, Nil, multipleAssets, "You have added 2 assets", "addAssets", Nil)(request, messages).toString

          application.stop()
        }

        "non-taxable" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = false))).build()

          val request = FakeRequest(GET, addAssetsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddAssetsView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(addNonTaxableAssetsForm, fakeDraftId, Nil, multipleAssets, "You have added 2 non-EEA companies",
              "addAssets.nonTaxable", Nil)(request, messages).toString

          application.stop()
        }
      }

      "YesNow selected" when {

        val indexOfNewAsset = 2

        "taxable" must {

          "set value in AddAssetsPage and not set value in WhatKindOfAssetPage" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))

            val uaCaptor:ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true)))
                .overrides(
                  bind[TrustsStoreService].to(mockTrustsStoreService)
                )
                .build()

            val request = FakeRequest(POST, addAnotherPostRoute)
              .withFormUrlEncodedBody(("value", YesNow.toString))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAssetsPage).get mustBe YesNow
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }
        }

        "non-taxable" must {

          "set values in AddAssetsPage and WhatKindOfAssetPage" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets()
                .copy(isTaxable = false)))
              .overrides(
                bind[TrustsStoreService].to(mockTrustsStoreService)
              ).build()

            val request = FakeRequest(POST, addAnotherPostRoute)
              .withFormUrlEncodedBody(("value", YesNow.toString))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAssetsPage).get mustBe YesNow
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)).get mustBe NonEeaBusiness

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }

          "not set value in WhatKindOfAssetPage when last asset is in progress" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            val application =
              applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets(InProgress).copy(isTaxable = false)))
                .overrides(
                  bind[TrustsStoreService].to(mockTrustsStoreService)
                ).build()

            val request = FakeRequest(POST, addAnotherPostRoute)
              .withFormUrlEncodedBody(("value", YesNow.toString))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAssetsPage).get mustBe YesNow
            uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

            application.stop()
          }
        }
      }

      "YesLater selected" must {

        "redirect to the next page when YesLater is submitted" when {

          val indexOfNewAsset = 2

          "asset in progress" must {

            "set in progress and redirect" in {
              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              when(mockRegistrationProgress.assetsStatus(any())).thenReturn(Some(InProgress))

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService),
                    bind[RegistrationProgress].to(mockRegistrationProgress)
                  ).build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.YesLater.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.YesLater
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

              application.stop()
            }

          }

          "taxable" must {

            "set in progress and not set value in WhatKindOfAssetPage" in {
              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService)
                  ).build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.YesLater.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.YesLater
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

              application.stop()
            }
          }

          "non-taxable" must {
            "set in progress and not set value in WhatKindOfAssetPage" in {

              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = false)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService)
                  )
                  .build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.YesLater.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.YesLater
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

              application.stop()
            }
          }
        }
      }

      "NoComplete selected" must {

        val indexOfNewAsset = 2

        "redirect to the next page when NoComplete is submitted" when {

          "asset in progress" must {

            "set in progress and redirect" in {
              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              when(mockRegistrationProgress.assetsStatus(any())).thenReturn(Some(InProgress))

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService),
                    bind[RegistrationProgress].to(mockRegistrationProgress)
                  ).build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.NoComplete.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.NoComplete
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.InProgress))(any(), any())

              application.stop()
            }

          }

          "taxable" must {

            "set task complete and not set value in WhatKindOfAssetPage" in {
              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = true)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService)
                  ).build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.NoComplete.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.NoComplete
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.Completed))(any(), any())

              application.stop()
            }
          }

          "non-taxable" must {
            "set task complete and not set value in WhatKindOfAssetPage" in {

              reset(registrationsRepository)
              when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
              val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

              val application =
                applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets().copy(isTaxable = false)))
                  .overrides(
                    bind[TrustsStoreService].to(mockTrustsStoreService)
                  )
                  .build()

              val request = FakeRequest(POST, addAnotherPostRoute)
                .withFormUrlEncodedBody(("value", AddAssets.NoComplete.toString))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
              uaCaptor.getValue.get(AddAssetsPage).get mustBe AddAssets.NoComplete
              uaCaptor.getValue.get(WhatKindOfAssetPage(indexOfNewAsset)) mustNot be(defined)

              verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.Completed))(any(), any())

              application.stop()
            }
          }
        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(userAnswersWithMultipleAssets())).build()

        val request =
          FakeRequest(POST, addAnotherPostRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = addTaxableAssetsForm.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[AddAssetsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual
          view(boundForm, fakeDraftId, Nil, multipleAssets, "You have added 2 assets", "addAssets", Nil)(request, messages).toString

        application.stop()
      }
    }

    "one asset type maxed out" must {

      "return OK and the correct view for a GET" in {

        val answers = emptyUserAnswers
          .set(WhatKindOfAssetPage(0), Money).success.value
          .set(AssetStatus(0), InProgress).success.value

        val rows = new AddAssetViewHelper(answers, fakeDraftId).rows

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val request = FakeRequest(GET, addAssetsRoute)

        val view = application.injector.instanceOf[AddAssetsView]

        val result = route(application, request).value

        status(result) mustEqual OK

        val content = contentAsString(result)

        content mustEqual
          view(addTaxableAssetsForm, fakeDraftId, rows.inProgress, rows.complete, "Add assets", "addAssets", List(("Money", 1)))(request, messages).toString

        content must include("You cannot add another money asset as you have entered a maximum of 1.")
        content must include("Check the assets you have added. If you have further assets to add within this type, write to HMRC with their details.")

        application.stop()
      }
    }

    "multiple asset types maxed out" must {

      def userAnswers: UserAnswers = {
        0.until(11).foldLeft(emptyUserAnswers.copy(isTaxable = true))((ua, i) => {
          ua
            .set(WhatKindOfAssetPage(i), i match {
              case x if 0 until 1 contains x => Money
              case x if 1 until 11 contains x => PropertyOrLand
            }).success.value
            .set(AssetStatus(i), InProgress).success.value
        })
      }

      "return OK and the correct view for a GET" in {

        val rows = new AddAssetViewHelper(userAnswers, fakeDraftId).rows

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val request = FakeRequest(GET, addAssetsRoute)

        val view = application.injector.instanceOf[AddAssetsView]

        val result = route(application, request).value

        status(result) mustEqual OK

        val content = contentAsString(result)

        content mustEqual
          view(addTaxableAssetsForm, fakeDraftId, rows.inProgress, rows.complete, "You have added 11 assets", "addAssets",
            List(("Money", 1), ("Property or land", 10)))(request, messages).toString

        content must include("You have entered the maximum number of assets for:")
        content must include("Check the assets you have added. If you have further assets to add within these types, write to HMRC with their details.")

        application.stop()
      }
    }

    "all asset types maxed out" when {

        "taxable" must {

          def userAnswers(max: Int, isTaxable: Boolean): UserAnswers = {
            0.until(max).foldLeft(emptyUserAnswers.copy(isTaxable = isTaxable))((ua, i) => {
              ua
                .set(WhatKindOfAssetPage(i), i match {
                  case x if 0 until 1 contains x => Money
                  case x if 1 until 11 contains x => PropertyOrLand
                  case x if 11 until 21 contains x => Shares
                  case x if 21 until 31 contains x => Business
                  case x if 31 until 41 contains x => Partnership
                  case x if 41 until 51 contains x => Other
                  case x if 51 until 76 contains x => NonEeaBusiness
                }).success.value
                .set(AssetStatus(i), InProgress).success.value
            })
          }

          val max: Int = 76

          val isTaxable: Boolean = true

          "return OK and the correct view for a GET" in {

            val answers = userAnswers(max, isTaxable)

            val rows = new AddAssetViewHelper(answers, fakeDraftId).rows

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            val request = FakeRequest(GET, addAssetsRoute)

            val view = application.injector.instanceOf[MaxedOutView]

            val result = route(application, request).value

            status(result) mustEqual OK

            val content = contentAsString(result)

            content mustEqual
              view(fakeDraftId, rows.inProgress, rows.complete, "You have added 76 assets", max, "addAssets")(request, messages).toString

            content must include("You cannot add another asset as you have entered a maximum of 76.")
            content must include("You can add another asset by removing an existing one, or write to HMRC with details of any additional assets.")

            application.stop()
          }

          "redirect to next page and set AddAssetsPage to NoComplete for a POST" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            when(mockRegistrationProgress.assetsStatus(any())).thenReturn(Some(Completed))

            val application = applicationBuilder(userAnswers = Some(userAnswers(max, isTaxable)))
              .overrides(
                bind[TrustsStoreService].to(mockTrustsStoreService),
                bind[RegistrationProgress].to(mockRegistrationProgress)
              )
              .build()

            val request = FakeRequest(POST, completePostRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAssetsPage).get mustBe NoComplete

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.Completed))(any(), any())

            application.stop()
          }
        }

        "non-taxable" must {

          def userAnswers(max: Int, isTaxable: Boolean): UserAnswers = {
            0.until(max).foldLeft(emptyUserAnswers.copy(isTaxable = isTaxable))((ua, i) => {
              ua
                .set(WhatKindOfAssetPage(i), NonEeaBusiness).success.value
                .set(AssetStatus(i), InProgress).success.value
            })
          }

          val max: Int = 25

          val isTaxable: Boolean = false

          "return OK and the correct view for a GET" in {

            val answers = userAnswers(max, isTaxable)

            val rows = new AddAssetViewHelper(answers, fakeDraftId).rows

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            val request = FakeRequest(GET, addAssetsRoute)

            val view = application.injector.instanceOf[MaxedOutView]

            val result = route(application, request).value

            status(result) mustEqual OK

            val content = contentAsString(result)

            content mustEqual
              view(fakeDraftId, rows.inProgress, rows.complete, "You have added 25 non-EEA companies", max, "addAssets.nonTaxable")(request, messages).toString

            content must include("You cannot add another non-EEA company as you have entered a maximum of 25.")
            content must include("You can add another non-EEA company by removing an existing one," +
              " or write to HMRC with details of any additional non-EEA companies.")

            application.stop()
          }

          "redirect to next page and set AddAssetsPage to NoComplete for a POST" in {

            reset(registrationsRepository)
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

            when(mockRegistrationProgress.assetsStatus(any())).thenReturn(Some(Completed))

            val application = applicationBuilder(userAnswers = Some(userAnswers(max, isTaxable)))
              .overrides(
                bind[TrustsStoreService].to(mockTrustsStoreService),
                bind[RegistrationProgress].to(mockRegistrationProgress)
              )
              .build()

            val request = FakeRequest(POST, completePostRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual fakeNavigator.desiredRoute.url

            verify(registrationsRepository).set(uaCaptor.capture)(any(), any())
            uaCaptor.getValue.get(AddAssetsPage).get mustBe NoComplete

            verify(mockTrustsStoreService).updateTaskStatus(mEq(draftId), mEq(TaskStatus.Completed))(any(), any())

            application.stop()
          }
        }

    }
  }
}
