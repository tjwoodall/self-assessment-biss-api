/*
 * Copyright 2025 HM Revenue & Customs
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

package v3.retrieveBiss.model.domain

import api.models.downstream.IncomeSourceType
import api.models.domain.TaxYear
import utils.enums.Enums

enum TypeOfBusiness(val preThresholdType: IncomeSourceType, val postThresholdType: Option[IncomeSourceType]) {
  case `uk-property`              extends TypeOfBusiness(IncomeSourceType.`uk-property`, Some(IncomeSourceType.`02`))
  case `uk-property-fhl`          extends TypeOfBusiness(IncomeSourceType.`fhl-property-uk`, None)
  case `foreign-property`         extends TypeOfBusiness(IncomeSourceType.`foreign-property`, Some(IncomeSourceType.`15`))
  case `foreign-property-fhl-eea` extends TypeOfBusiness(IncomeSourceType.`fhl-property-eea`, None)
  case `self-employment`          extends TypeOfBusiness(IncomeSourceType.`self-employment`, Some(IncomeSourceType.`01`))

  def toIncomeSourceType(taxYear: Int): IncomeSourceType =
    if (taxYear >= TypeOfBusiness.postThresholdTaxYear.year) {
      postThresholdType.getOrElse(
        throw new IllegalArgumentException(
          s"Unsupported income source type: $preThresholdType for tax year: $taxYear"
        )
      )
    } else {
      preThresholdType
    }

}

object TypeOfBusiness {
  val fhlPropertyMinimumTaxYear: TaxYear = TaxYear.fromMtd("2025-26")
  val postThresholdTaxYear: TaxYear      = TaxYear.fromMtd("2025-26")

  val parser: PartialFunction[String, TypeOfBusiness] =
    Enums.parser(values)

}
