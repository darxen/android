
typedef void* pointer;

typedef unsigned long(*HashFunc)(pointer);
typedef int(*EqualsFunc)(pointer,pointer);

typedef struct _HashTable       HashTable;

HashTable* hash_table_new       (HashFunc hashFunc, EqualsFunc equalsFunc);
void       hash_table_free      (HashTable* table);
void       hash_table_insert    (HashTable* table, pointer key, pointer val);
pointer    hash_table_lookup    (HashTable* table, pointer key);
pointer    hash_table_remove    (HashTable* table, pointer key);

