#ifndef endlesstunnel_input_util_hpp
#define endlesstunnel_input_util_hpp

#include "engine.hpp"
#include "our_key_codes.hpp"

// event type
#define COOKED_EVENT_TYPE_JOY 0
#define COOKED_EVENT_TYPE_POINTER_DOWN 1
#define COOKED_EVENT_TYPE_POINTER_UP 2
#define COOKED_EVENT_TYPE_POINTER_MOVE 3
#define COOKED_EVENT_TYPE_KEY_DOWN 4
#define COOKED_EVENT_TYPE_KEY_UP 5
#define COOKED_EVENT_TYPE_BACK 6

struct CookedEvent {
    int type;

    // for joystick events:
    float joyX, joyY;

    // for pointer events
    int motionPointerId;
    bool motionIsOnScreen;
    float motionX, motionY;
    float motionMinX, motionMaxX;
    float motionMinY, motionMaxY;

    // for key events
    int keyCode;
};

typedef bool (*CookedEventCallback)(struct CookedEvent *event);
bool CookEvent(AInputEvent *event, CookedEventCallback callback);

#endif

