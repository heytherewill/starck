#include <jni.h>
#include <android/bitmap.h>

constexpr unsigned int greenShift = 8;
constexpr unsigned int blueShift = 16;
constexpr unsigned int redMask = 0x00000FF;
constexpr unsigned int blueMask = 0x00FF0000;
constexpr unsigned int greenMask = 0x0000FF00;

constexpr int getRed(unsigned int color) { return (color & redMask); }

constexpr int getBlue(unsigned int color) { return ((color & blueMask) >> blueShift); }

constexpr int getGreen(unsigned int color) { return ((color & greenMask) >> greenShift); }

constexpr uint32_t getColor(unsigned int red, unsigned int green, unsigned int blue) {
    return ((blue << blueShift) & blueMask) |
           ((green << greenShift) & greenMask) |
           (red & redMask);
}

extern "C"
#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
JNIEXPORT void JNICALL Java_com_heytherewill_starck_processing_ImageProcessor_stackBitmaps(
        JNIEnv *env, jclass, jobject stack, jobjectArray bitmaps) {

    uint32_t *line;
    AndroidBitmapInfo stackBitmapInfo;

    if (AndroidBitmap_getInfo(env, stack, &stackBitmapInfo) < 0)
        return;

    auto stackWidth = stackBitmapInfo.width;
    auto stackHeight = stackBitmapInfo.height;
    auto pixelArraySize = stackWidth * stackHeight;
    auto redStackPixels = new uint32_t[pixelArraySize];
    auto blueStackPixels = new uint32_t[pixelArraySize];
    auto greenStackPixels = new uint32_t[pixelArraySize];

    auto numberOfBitmaps = env->GetArrayLength(bitmaps);

    for (int i = 0; i < numberOfBitmaps; ++i) {

        auto bitmap = env->GetObjectArrayElement(bitmaps, i);

        void *bitmapPixels;
        AndroidBitmapInfo bitmapInfo;
        if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0)
            return;

        if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0)
            return;

        auto bitmapWidth = bitmapInfo.width;
        auto bitmapHeight = bitmapInfo.height;

        for (int y = 0; y < bitmapHeight; ++y) {
            line = (uint32_t *) bitmapPixels;

            for (int x = 0; x < bitmapWidth; ++x) {


                auto color = line[x];
                int blue = getBlue(color);
                int green = getGreen(color);
                int red = getRed(color);

                auto index = (y * stackWidth) + x;
                redStackPixels[index] += red;
                blueStackPixels[index] += blue;
                greenStackPixels[index] += green;
            }

            bitmapPixels = (char *) bitmapPixels + bitmapInfo.stride;
        }

        AndroidBitmap_unlockPixels(env, bitmap);
    }


    void *stackPixels;
    if (AndroidBitmap_lockPixels(env, stack, &stackPixels) < 0)
        return;

    for (int y = 0; y < stackHeight; y++) {
        line = (uint32_t *) stackPixels;
        for (int x = 0; x < stackWidth; x++) {

            auto index = (y * stackWidth) + x;
            auto red = redStackPixels[index] / numberOfBitmaps;
            auto blue = blueStackPixels[index] / numberOfBitmaps;
            auto green = greenStackPixels[index] / numberOfBitmaps;

            line[x] = getColor(red, green, blue);
        }

        stackPixels = (char *) stackPixels + stackBitmapInfo.stride;
    }

    AndroidBitmap_unlockPixels(env, stack);
}
#pragma clang diagnostic pop