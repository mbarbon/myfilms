package org.barbon.myfilms;

import _root_.android.app.Activity;
import _root_.android.view.View;
import _root_.android.widget.{Button, TextView, EditText};

package object helpers {
    implicit def func$Clicker0(f : () => Unit) =
        new View.OnClickListener { def onClick(v : View) = f(); };

    implicit def view$RichView(v : View) = new RichView(v)
}

trait ActivityHelper { self : Activity =>
    def findView[T <: View](id : Int) : T =
        self.findViewById(id).asInstanceOf[T];

    def findButton(id : Int) = findView[Button](id);
    def findTextView(id : Int) = findView[TextView](id);
    def findEditText(id : Int) = findView[EditText](id);
}

class RichView(private val view : View) {
    def findView[T <: View](id : Int) : T =
        view.findViewById(id).asInstanceOf[T];

    def findButton(id : Int) = findView[Button](id);
    def findTextView(id : Int) = findView[TextView](id);
    def findEditText(id : Int) = findView[EditText](id);
}
