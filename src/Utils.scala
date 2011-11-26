package org.barbon.myfilms;

import _root_.android.app.Activity;

package object helpers {
    import _root_.android.view.View;

    implicit def funcToClicker0(f : () => Unit) =
        new View.OnClickListener { def onClick(v : View) = f(); };
}

trait ActivityHelper { self : Activity =>
    import _root_.android.widget.{Button, TextView, EditText};
    import _root_.android.view.View;

    def findView[T <: View](id : Int) : T =
        self.findViewById(id).asInstanceOf[T];

    def findButton(id : Int) =
        findView[Button](id);

    def findTextView(id : Int) =
        findView[TextView](id);

    def findEditText(id : Int) =
        findView[EditText](id);
}
