package com.liskovsoft.leankeyboard.addons.voice;

import android.content.Intent;

interface ActivityListener {
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
