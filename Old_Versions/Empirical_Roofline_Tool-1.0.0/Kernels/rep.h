#ifndef REP_H
#define REP_H

#define REP2(S)        S ;       S
#define REP4(S)   REP2(S);  REP2(S)
#define REP8(S)   REP4(S);  REP4(S)
#define REP16(S)  REP8(S);  REP8(S)
#define REP32(S) REP16(S); REP16(S)
#define REP64(S) REP32(S); REP32(S)

#endif 
