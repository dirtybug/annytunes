package com.app.annytunes.uart;




    public   class Bank {
        private static final int RECORD_SIZE = 64;
		public final long address;      // endereço inicial do banco
        public final int  sizeBytes; // bytes válidos para canais neste banco
        public final int  channels;  // quantos registos são canais (último banco ≠ size/64)
        public int startChannel;
        public int endChannel;

        public Bank(long base, int sizeBytes, int channels, int startCh, int endCh) {
            this.address = base;
            this.sizeBytes = sizeBytes;
            this.channels = channels;
            this.startChannel = startCh;
            this.endChannel = endCh;
        }

        /** nº de registos de 64B presentes (sizeBytes / 64) */
        public int recordCount() { return sizeBytes / RECORD_SIZE; }

        /** endereço do início do registo 'recIndex' (0..recordCount-1) */
        public long addressOfRecord(int recIndex) {
            if (recIndex < 0 || recIndex >= recordCount())
                throw new IndexOutOfBoundsException("recIndex=" + recIndex);
            return address + (long) recIndex * RECORD_SIZE;
        }
    }
