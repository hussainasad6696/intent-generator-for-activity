@GenerateIntent(
    target = MainActivity::class,
    resultCode = 1002,
    params = [
        Param("intList", ArrayList::class, Int::class),
        Param("stringList", ArrayList::class, String::class),
        Param("charSeqList", ArrayList::class, CharSequence::class),
        Param("uriList", ArrayList::class, Uri::class, isNullable = false), Non null are those values without which the activity will not function
        Param("customParcelableList", ArrayList::class, Demi::class),
        Param("customParcelable", Demi::class),
        Param("intValue", Int::class),
        Param("longValue", Long::class),
        Param("floatValue", Float::class),
        Param("doubleValue", Double::class),
        Param("booleanValue", Boolean::class),
        Param("shortValue", Short::class),
        Param("byteValue", Byte::class),
        Param("charValue", Char::class)
    ]
)

