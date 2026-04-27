import os
AT = chr(64)
os.chdir("/home/ec2-user/Q/StatusSaver")

# 1. Recreate activity_home.xml safely
h = [
'<?xml version="1.0" encoding="utf-8"?>',
'<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="' + AT + 'color/background">',
'<com.google.android.material.appbar.MaterialToolbar android:id="' + AT + '+id/toolbar" android:layout_width="match_parent" android:layout_height="?attr/actionBarSize" android:background="' + AT + 'color/primary" app:titleTextColor="' + AT + 'android:color/white"/>',
'<TextView android:id="' + AT + '+id/tvSyncStatus" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="#E8F5E9" android:padding="8dp" android:gravity="center" android:text="Syncing..." android:textSize="12sp" android:textColor="#388E3C" android:visibility="gone"/>',
'<com.google.android.material.tabs.TabLayout android:id="' + AT + '+id/tabLayout" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="' + AT + 'color/surface" app:tabSelectedTextColor="' + AT + 'color/primary" app:tabIndicatorColor="' + AT + 'color/primary" app:tabMode="fixed"/>',
'<androidx.swiperefreshlayout.widget.SwipeRefreshLayout android:id="' + AT + '+id/swipeRefreshLayout" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"><FrameLayout android:layout_width="match_parent" android:layout_height="match_parent">',
'<androidx.recyclerview.widget.RecyclerView android:id="' + AT + '+id/recyclerViewImages" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="2dp" android:clipToPadding="false" android:visibility="gone"/>',
'<androidx.recyclerview.widget.RecyclerView android:id="' + AT + '+id/recyclerViewVideos" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="2dp" android:clipToPadding="false" android:visibility="gone"/>',
'<androidx.recyclerview.widget.RecyclerView android:id="' + AT + '+id/recyclerViewSaved" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="2dp" android:clipToPadding="false" android:visibility="gone"/>',
'<com.facebook.shimmer.ShimmerFrameLayout android:id="' + AT + '+id/shimmerImages" android:layout_width="match_parent" android:layout_height="match_parent" android:visibility="gone"><LinearLayout android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical"><include layout="layout/item_shimmer"/><include layout="layout/item_shimmer"/><include layout="layout/item_shimmer"/></LinearLayout></com.facebook.shimmer.ShimmerFrameLayout>',
'<include layout="layout/layout_empty" android:id="' + AT + '+id/layoutEmpty" android:visibility="gone"/>',
'<include layout="layout/layout_error" android:id="' + AT + '+id/layoutError" android:visibility="gone"/>',
'</FrameLayout></androidx.swiperefreshlayout.widget.SwipeRefreshLayout>',
'<com.google.android.gms.ads.AdView android:id="' + AT + '+id/adView" android:layout_width="match_parent" android:layout_height="wrap_content" app:adSize="BANNER" app:adUnitId="ca-app-pub-1607968585289432/8779090448"/>',
'</LinearLayout>'
]
with open("app/src/main/res/layout/activity_home.xml", "w") as f: f.write("\n".join(h))

# 2. Fix layout_empty.xml
e = [
'<?xml version="1.0" encoding="utf-8"?>',
'<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="32dp">',
'<TextView android:layout_width="80dp" android:layout_height="80dp" android:gravity="center" android:text="😅" android:textSize="40sp" android:layout_marginBottom="16dp"/>',
'<TextView android:id="' + AT + '+id/tvEmptyMessage" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="16sp" android:textColor="' + AT + 'color/text_secondary" android:gravity="center"/>',
'</LinearLayout>'
]
with open("app/src/main/res/layout/layout_empty.xml", "w") as f: f.write("\n".join(e))

# 3. Fix layout_error.xml
r = [
'<?xml version="1.0" encoding="utf-8"?>',
'<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="32dp">',
'<TextView android:layout_width="80dp" android:layout_height="80dp" android:gravity="center" android:text="⚠️" android:textSize="40sp" android:layout_marginBottom="16dp"/>',
'<TextView android:id="' + AT + '+id/tvErrorMessage" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="16sp" android:textColor="' + AT + 'color/error" android:gravity="center" android:layout_marginBottom="24dp"/>',
'<com.google.android.material.button.MaterialButton android:id="' + AT + '+id/btnRetry" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="' + AT + 'string/retry"/>',
'</LinearLayout>'
]
with open("app/src/main/res/layout/layout_error.xml", "w") as f: f.write("\n".join(r))

print("SUCCESS: All layouts rebuilt with safe symbols!")
