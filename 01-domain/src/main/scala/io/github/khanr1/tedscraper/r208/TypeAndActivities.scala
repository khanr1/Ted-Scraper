package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.RichText
case class TypeAndActivities(
    authorityType: Option[RichText],
    authorityOther: Option[RichText],
    activities: List[RichText],
    activityOther: Option[RichText]
)
