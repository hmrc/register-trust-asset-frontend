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

package models

import viewmodels.RadioOption

sealed trait ShareClass

object ShareClass extends Enumerable.Implicits {

  case object Ordinary extends WithName("ordinary") with ShareClass
  case object NonVoting extends WithName("non-voting") with ShareClass
  case object Redeemable extends WithName("redeemable") with ShareClass
  case object Preference extends WithName("preference") with ShareClass
  case object Deferred extends WithName("deferred") with ShareClass
  case object Management extends WithName("management") with ShareClass
  case object OtherClasses extends WithName("other-classes") with ShareClass
  case object Voting extends WithName("voting") with ShareClass
  case object Dividend extends WithName("dividend") with ShareClass
  case object Capital extends WithName("capital") with ShareClass
  case object Growth extends WithName("growth") with ShareClass
  case object Other extends WithName("other") with ShareClass

  val allValues: List[ShareClass] = List(
    Ordinary,
    Preference,
    Deferred,
    Growth,
    Other
  )

  def asRadioOptions(list: List[ShareClass]): List[RadioOption] = list.map { value =>
    RadioOption("shares.class", value.toString)
  }

  val allOptions: List[RadioOption] = asRadioOptions(allValues)

  implicit val enumerable: Enumerable[ShareClass] =
    Enumerable(allValues.map(v => v.toString -> v): _*)

  def toDES(value: ShareClass): String            = value match {
    case Ordinary     => "Ordinary shares"
    case NonVoting    => "Non-voting shares"
    case Redeemable   => "Redeemable shares"
    case Preference   => "Preference shares"
    case Deferred     => "Deferred ordinary shares"
    case Management   => "Management shares"
    case OtherClasses => "Other classes of shares"
    case Voting       => "Voting shares"
    case Dividend     => "Dividend shares"
    case Capital      => "Capital share"
    case Growth       => "Other"
    case Other        => "Other"
  }
}
