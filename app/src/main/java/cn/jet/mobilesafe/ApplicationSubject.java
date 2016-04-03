package cn.jet.mobilesafe;

/**
 * Created by jerry on 2/26/2016.
 */

public class ApplicationSubject extends Subject {
    public void exit(){
        notifyObservers(Observer.mExitType);
    }
}