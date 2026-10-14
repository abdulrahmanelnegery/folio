-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class dev.folio.reader.**$$serializer { *; }
-keepclassmembers class dev.folio.reader.** { *** Companion; }
# PDFBox (writes the edits into a copy of the PDF): JPEG 2000 decoding is an optional extra we do not ship.
-dontwarn com.gemalto.jp2.**
