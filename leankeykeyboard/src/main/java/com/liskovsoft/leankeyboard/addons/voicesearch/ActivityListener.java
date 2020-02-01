package com.liskovsoft.leankeyboard.addons.voicesearch;

import android.content.Intent;

interface ActivityListener {
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
