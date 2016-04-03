package cn.jet.mobilesafe;

/**
 * Created by jerry on 2/26/2016.
 */
import java.util.ArrayList;
import java.util.List;
import cn.jet.mobilesafe.Observer;


public class Subject {
    private List<Observer> observers = new ArrayList<Observer>();

    public void attach(Observer observer){
        observers.add(observer);
    }

    public void detach(Observer observer){
        observers.remove(observer);
    }

    protected void notifyObservers(int nType){
        for(Observer observer : observers){
            observer.notify(this, nType);
        }
    }
}