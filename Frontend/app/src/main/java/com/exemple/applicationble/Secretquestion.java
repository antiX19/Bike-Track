package com.exemple.applicationble;

public class Secretquestion {
    private String question;

    private int id;

    public void Secretquestion(String question){
        this.question = question;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getQuestion() {
        return question;
    }

    @Override
    public String toString() {
        return question; // ou formater selon vos besoins
    }
    public void setQuestion(String question) {
        this.question = question;
    }
}

