:- use_module(library(jpl)).

test(J) :-
    jpl_call(J, 'stateEngage', [], R).

p(a).

close :-
    halt.



t :-
    throw( 'this is an error message').

display(X) :-
    write( X).
